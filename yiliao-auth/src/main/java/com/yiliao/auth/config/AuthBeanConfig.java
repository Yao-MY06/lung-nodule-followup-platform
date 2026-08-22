package com.yiliao.auth.config;

import com.yiliao.common.data.fill.UserContextProvider;
import com.yiliao.common.core.constant.YiliaoConstants;
import com.yiliao.common.security.jwt.JwtProperties;
import com.yiliao.common.security.jwt.JwtTokenService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class AuthBeanConfig {

    @Bean
    public JwtTokenService jwtTokenService(JwtProperties properties) {
        return new JwtTokenService(properties);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /** 操作人填充：取网关透传的 X-User-Id（specs/global/20 §1）。 */
    @Bean
    public UserContextProvider userContextProvider() {
        return () -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                String userId = attributes.getRequest().getHeader(YiliaoConstants.HEADER_USER_ID);
                if (userId != null && !userId.isBlank()) {
                    return Long.valueOf(userId);
                }
            }
            return null;
        };
    }
}
