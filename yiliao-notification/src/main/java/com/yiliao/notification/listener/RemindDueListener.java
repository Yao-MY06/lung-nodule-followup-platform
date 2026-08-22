package com.yiliao.notification.listener;

import com.yiliao.common.mq.constant.Topics;
import com.yiliao.common.mq.event.BaseEvent;
import com.yiliao.common.mq.event.RemindPayload;
import com.yiliao.notification.service.MessageService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * REMIND_DUE 消费者（specs/flows/F2）：幂等三保险见 MessageService；
 * 消费异常向上抛出触发 MQ 重试（3 次后进死信，broker 端配置）。
 */
@Component
@RocketMQMessageListener(topic = Topics.REMIND_DUE, consumerGroup = "yiliao-notification-remind")
public class RemindDueListener implements RocketMQListener<BaseEvent<RemindPayload>> {

    private static final Logger log = LoggerFactory.getLogger(RemindDueListener.class);

    private final MessageService messageService;

    public RemindDueListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void onMessage(BaseEvent<RemindPayload> event) {
        log.info("收到提醒事件 bizKey={} payload={}", event.bizKey(), event.payload());
        messageService.consumeRemind(event.payload(), event.bizKey());
    }
}
