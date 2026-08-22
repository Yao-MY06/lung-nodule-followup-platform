package com.yiliao.common.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置（specs/global/10 §5）。secret 生产环境走 Nacos/环境变量覆盖，仓库内仅为开发占位。
 * auth 与 gateway 必须配置相同的 secret 与 issuer。
 */
@ConfigurationProperties(prefix = "yiliao.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        long accessTtlSeconds,
        long refreshTtlSeconds
) {
}
