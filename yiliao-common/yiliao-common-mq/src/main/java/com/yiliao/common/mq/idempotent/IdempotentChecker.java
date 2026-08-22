package com.yiliao.common.mq.idempotent;

import com.yiliao.common.core.constant.YiliaoConstants;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 消费幂等检查器（specs/global/20 §3）：Redis SETNX，TTL 7 天。
 * 与 message_record 唯一键构成双保险——Redis 不可用时由 DB 唯一键兜底。
 */
public class IdempotentChecker {

    private final StringRedisTemplate redisTemplate;

    public IdempotentChecker(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 首次见到该事件返回 true 并落标记；重复返回 false。 */
    public boolean tryConsume(String eventType, String bizKey) {
        String key = YiliaoConstants.MQ_CONSUMED_PREFIX + eventType + ":" + bizKey;
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofDays(YiliaoConstants.MQ_CONSUMED_TTL_DAYS));
        return Boolean.TRUE.equals(first);
    }

    /** 消费失败时释放标记，允许 MQ 重投后重试。 */
    public void release(String eventType, String bizKey) {
        redisTemplate.delete(YiliaoConstants.MQ_CONSUMED_PREFIX + eventType + ":" + bizKey);
    }
}
