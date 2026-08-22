package com.yiliao.gateway.filter;

import com.yiliao.common.core.constant.YiliaoConstants;
import com.yiliao.common.security.jwt.JwtProperties;
import com.yiliao.common.security.jwt.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthGlobalFilterTest {

    private static final String SECRET = "unit-test-secret-key-0123456789abcdef0123456789";

    private JwtTokenService jwtTokenService;
    private ReactiveStringRedisTemplate redisTemplate;
    private AuthGlobalFilter filter;
    private final AtomicReference<ServerWebExchange> chainCaptured = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(new JwtProperties(SECRET, "yiliao", 7200, 604800));
        redisTemplate = mock(ReactiveStringRedisTemplate.class);
        filter = new AuthGlobalFilter(jwtTokenService, redisTemplate);
        chainCaptured.set(null);
    }

    private GatewayFilterChain capturingChain() {
        return exchange -> {
            chainCaptured.set(exchange);
            return Mono.empty();
        };
    }

    private String accessToken() {
        return jwtTokenService.issueAccessToken(7L, "doctor01", 2, List.of("DOCTOR")).token();
    }

    @Test
    void loginWhitelistedWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/login").build());
        filter.filter(exchange, capturingChain()).block();
        assertNotNull(chainCaptured.get(), "白名单请求应放行");
        assertNotNull(chainCaptured.get().getRequest().getHeaders().getFirst(YiliaoConstants.HEADER_TRACE_ID));
    }

    @Test
    void internalPathGets404() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/internal/users/1").build());
        filter.filter(exchange, capturingChain()).block();
        assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
        assertEquals(null, chainCaptured.get());
    }

    @Test
    void missingTokenGets401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/followup/workbench").build());
        filter.filter(exchange, capturingChain()).block();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void garbageTokenGets401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/followup/workbench")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt").build());
        filter.filter(exchange, capturingChain()).block();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void blacklistedTokenGets401() {
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(true));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/followup/workbench")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken()).build());
        filter.filter(exchange, capturingChain()).block();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void validTokenRelaysUserHeaders() {
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/followup/workbench")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken()).build());
        filter.filter(exchange, capturingChain()).block();
        assertNotNull(chainCaptured.get(), "有效 Token 应放行");
        HttpHeaders headers = chainCaptured.get().getRequest().getHeaders();
        assertEquals("7", headers.getFirst(YiliaoConstants.HEADER_USER_ID));
        assertEquals("DOCTOR", headers.getFirst(YiliaoConstants.HEADER_USER_ROLES));
        assertTrue(headers.getFirst(YiliaoConstants.HEADER_TRACE_ID) != null);
    }

    @Test
    void redisFailureFailsOpen() {
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.error(new RuntimeException("redis down")));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/followup/workbench")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken()).build());
        filter.filter(exchange, capturingChain()).block();
        assertNotNull(chainCaptured.get(), "Redis 故障应 fail-open 放行");
    }
}
