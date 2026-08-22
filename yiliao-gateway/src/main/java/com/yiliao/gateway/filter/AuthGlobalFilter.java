package com.yiliao.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiliao.common.core.constant.YiliaoConstants;
import com.yiliao.common.core.error.CommonErrorCode;
import com.yiliao.common.core.exception.BizException;
import com.yiliao.common.core.result.Result;
import com.yiliao.common.security.jwt.JwtTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * JWT 全局过滤器（specs/modules/gateway.md §4）：
 * 白名单放行；internal 路径拒绝外部访问；无/坏 Token→401；黑名单→401；有效→注入 X-User-* 与 X-Trace-Id。
 * Redis 黑名单校验 fail-open（Redis 故障放行并告警，可用性优先，见模块文档 §5）。
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthGlobalFilter.class);

    /** 免鉴权白名单（specs/modules/gateway.md §3） */
    private static final String[] WHITELIST = {
            "/api/auth/login", "/api/auth/refresh",
            "/doc.html", "/webjars/**", "/v3/api-docs/**", "/favicon.ico"
    };

    /** 服务间内部接口，外部一律 404（specs/global/30 §5） */
    private static final String INTERNAL_PATTERN = "/api/*/internal/**";

    private final JwtTokenService jwtTokenService;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthGlobalFilter(JwtTokenService jwtTokenService,
                            ReactiveStringRedisTemplate redisTemplate) {
        this.jwtTokenService = jwtTokenService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (pathMatcher.match(INTERNAL_PATTERN, path)) {
            return writeResult(exchange, HttpStatus.NOT_FOUND, Result.fail(CommonErrorCode.NOT_FOUND));
        }
        if (isWhitelisted(path)) {
            return chain.filter(withTraceId(exchange));
        }

        String token = extractBearerToken(exchange.getRequest().getHeaders());
        if (token == null) {
            return writeResult(exchange, HttpStatus.UNAUTHORIZED,
                    Result.fail(CommonErrorCode.UNAUTHORIZED));
        }

        JwtTokenService.TokenPayload payload;
        try {
            payload = jwtTokenService.parse(token, JwtTokenService.TYPE_ACCESS);
        } catch (BizException e) {
            return writeResult(exchange, HttpStatus.UNAUTHORIZED, Result.fail(e.getErrorCode()));
        }

        String blacklistKey = YiliaoConstants.TOKEN_BLACKLIST_PREFIX + payload.jti();
        return redisTemplate.hasKey(blacklistKey)
                .onErrorResume(ex -> {
                    // fail-open：Redis 故障放行（黑名单是少数场景），告警观测
                    log.error("黑名单校验失败，fail-open 放行 key={}", blacklistKey, ex);
                    return Mono.just(false);
                })
                .flatMap(blacklisted -> {
                    if (Boolean.TRUE.equals(blacklisted)) {
                        return writeResult(exchange, HttpStatus.UNAUTHORIZED,
                                Result.fail(CommonErrorCode.UNAUTHORIZED));
                    }
                    ServerWebExchange mutated = withTraceId(exchange).mutate()
                            .request(builder -> {
                                builder.header(YiliaoConstants.HEADER_USER_ID, String.valueOf(payload.userId()));
                                builder.header(YiliaoConstants.HEADER_USER_ROLES, String.join(",", payload.roles()));
                            })
                            .build();
                    return chain.filter(mutated);
                });
    }

    @Override
    public int getOrder() {
        // 限流过滤器（后续 Sentinel）之后、业务转发之前
        return -100;
    }

    private boolean isWhitelisted(String path) {
        for (String pattern : WHITELIST) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private String extractBearerToken(HttpHeaders headers) {
        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")
                && authorization.length() > 7) {
            return authorization.substring(7);
        }
        return null;
    }

    /** 透传或生成 X-Trace-Id，全链路可追踪（specs/global/10 §8）。 */
    private ServerWebExchange withTraceId(ServerWebExchange exchange) {
        String traceId = exchange.getRequest().getHeaders().getFirst(YiliaoConstants.HEADER_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        String finalTraceId = traceId;
        return exchange.mutate()
                .request(builder -> builder.header(YiliaoConstants.HEADER_TRACE_ID, finalTraceId))
                .build();
    }

    private Mono<Void> writeResult(ServerWebExchange exchange, HttpStatus status, Result<Void> result) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(result);
        } catch (JsonProcessingException e) {
            body = "{\"code\":500,\"msg\":\"系统繁忙\"}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}
