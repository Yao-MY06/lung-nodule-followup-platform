package com.yiliao.common.mq.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * MQ 事件基类（specs/global/20 §3）。主题契约见 Topics；消费幂等键 = eventType + bizKey。
 */
public record BaseEvent<T>(String eventId,
                           String eventType,
                           String bizKey,
                           String source,
                           LocalDateTime occurredAt,
                           T payload) {

    public static <T> BaseEvent<T> of(String eventType, String bizKey, String source, T payload) {
        return new BaseEvent<>(UUID.randomUUID().toString(), eventType, bizKey, source,
                LocalDateTime.now(), payload);
    }
}
