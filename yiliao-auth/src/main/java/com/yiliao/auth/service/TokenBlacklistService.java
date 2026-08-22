package com.yiliao.auth.service;

import com.yiliao.common.core.constant.YiliaoConstants;
import com.yiliao.common.security.jwt.JwtTokenService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Token 黑名单（specs/global/10 §5）：key = yiliao:auth:blacklist:{jti}，TTL=剩余有效期。
 */
@Service
public class TokenBlacklistService {

    static String key(String jti) {
        return YiliaoConstants.TOKEN_BLACKLIST_PREFIX + jti;
    }

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 注销/刷新时拉黑；已过期 Token 无需拉黑。 */
    public void blacklist(JwtTokenService.TokenPayload payload) {
        long remaining = payload.expiresAt().getTime() - System.currentTimeMillis();
        if (remaining > 0) {
            redisTemplate.opsForValue().set(key(payload.jti()), "1", Duration.ofMillis(remaining));
        }
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(jti)));
    }
}
