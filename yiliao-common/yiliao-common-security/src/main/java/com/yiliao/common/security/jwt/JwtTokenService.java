package com.yiliao.common.security.jwt;

import com.yiliao.common.core.error.CommonErrorCode;
import com.yiliao.common.core.exception.BizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT 签发与解析（specs/global/10 §5）：access/refresh 双 Token，jti 唯一，HS256。
 * 签发方：auth-service；校验方：gateway 与各服务。任何失败统一抛 UNAUTHORIZED(401)。
 */
public class JwtTokenService {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtTokenService(JwtProperties properties) {
        if (properties == null || properties.secret() == null
                || properties.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("yiliao.jwt.secret 必须至少 32 字节（HS256 要求）");
        }
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.properties = properties;
    }

    public TokenPayload issueAccessToken(Long userId, String username, Integer userType, List<String> roles) {
        return issue(userId, username, userType, roles, TYPE_ACCESS, properties.accessTtlSeconds());
    }

    public TokenPayload issueRefreshToken(Long userId, String username, Integer userType, List<String> roles) {
        return issue(userId, username, userType, roles, TYPE_REFRESH, properties.refreshTtlSeconds());
    }

    /** 解析并校验签名/有效期/签发方。 */
    public TokenPayload parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return toPayload(claims);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    /** 解析并校验 Token 类型（refresh 端点防 access Token 复用）。 */
    public TokenPayload parse(String token, String expectedType) {
        TokenPayload payload = parse(token);
        if (!expectedType.equals(payload.tokenType())) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED);
        }
        return payload;
    }

    /** 剩余有效秒数（注销黑名单 TTL 用；已过期返回 0）。 */
    public long remainingSeconds(TokenPayload payload) {
        long remaining = (payload.expiresAt().getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    private TokenPayload issue(Long userId, String username, Integer userType, List<String> roles,
                               String type, long ttlSeconds) {
        long now = System.currentTimeMillis();
        Date expiresAt = new Date(now + ttlSeconds * 1000);
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .id(jti)
                .subject(String.valueOf(userId))
                .issuer(properties.issuer())
                .issuedAt(new Date(now))
                .expiration(expiresAt)
                .claim("uname", username)
                .claim("utype", userType)
                .claim("roles", String.join(",", roles))
                .claim("type", type)
                .signWith(key)
                .compact();
        return new TokenPayload(jti, userId, username, userType, List.copyOf(roles), type, token, expiresAt);
    }

    private TokenPayload toPayload(Claims claims) {
        String rolesClaim = claims.get("roles", String.class);
        List<String> roles = (rolesClaim == null || rolesClaim.isBlank())
                ? List.of()
                : List.of(rolesClaim.split(","));
        return new TokenPayload(
                claims.getId(),
                Long.valueOf(claims.getSubject()),
                claims.get("uname", String.class),
                claims.get("utype", Integer.class),
                roles,
                claims.get("type", String.class),
                null,
                claims.getExpiration());
    }

    /**
     * Token 载荷。token 字段仅签发方向填入，解析结果为 null。
     */
    public record TokenPayload(
            String jti,
            Long userId,
            String username,
            Integer userType,
            List<String> roles,
            String tokenType,
            String token,
            Date expiresAt
    ) {
    }
}
