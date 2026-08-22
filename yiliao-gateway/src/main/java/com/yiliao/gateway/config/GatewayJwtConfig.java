package com.yiliao.gateway.config;

import com.yiliao.common.security.jwt.JwtProperties;
import com.yiliao.common.security.jwt.JwtTokenService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关侧 JWT 校验器：与 auth 共用 common-security 同一实现（specs/global/30 §2，禁止复制解析逻辑）。
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class GatewayJwtConfig {

    @Bean
    public JwtTokenService jwtTokenService(JwtProperties properties) {
        return new JwtTokenService(properties);
    }
}
