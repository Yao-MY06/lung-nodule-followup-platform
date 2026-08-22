package com.yiliao.common.mq.config;

import com.yiliao.common.mq.idempotent.IdempotentChecker;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * MQ 公共自动配置：幂等检查器。
 */
@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
public class YiliaoMqAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IdempotentChecker idempotentChecker(StringRedisTemplate redisTemplate) {
        return new IdempotentChecker(redisTemplate);
    }
}
