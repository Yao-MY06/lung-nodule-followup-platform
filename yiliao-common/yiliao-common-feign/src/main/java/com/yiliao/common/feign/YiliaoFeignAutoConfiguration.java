package com.yiliao.common.feign;

import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Feign 自动配置：注册内部头透传拦截器。
 */
@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
public class YiliaoFeignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(HeaderRelayInterceptor.class)
    public HeaderRelayInterceptor headerRelayInterceptor() {
        return new HeaderRelayInterceptor();
    }
}
