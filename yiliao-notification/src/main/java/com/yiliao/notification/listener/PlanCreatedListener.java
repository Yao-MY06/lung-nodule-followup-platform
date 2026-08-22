package com.yiliao.notification.listener;

import com.yiliao.common.mq.constant.Topics;
import com.yiliao.common.mq.event.BaseEvent;
import com.yiliao.common.mq.event.PlanCreatedPayload;
import com.yiliao.notification.service.MessageService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * PLAN_CREATED 消费者：通知性站内信（随访计划已生成），重复事件被幂等键拦截。
 */
@Component
@RocketMQMessageListener(topic = Topics.PLAN_CREATED, consumerGroup = "yiliao-notification-plan")
public class PlanCreatedListener implements RocketMQListener<BaseEvent<PlanCreatedPayload>> {

    private static final Logger log = LoggerFactory.getLogger(PlanCreatedListener.class);

    private final MessageService messageService;

    public PlanCreatedListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void onMessage(BaseEvent<PlanCreatedPayload> event) {
        PlanCreatedPayload payload = event.payload();
        messageService.sendInternal(payload.patientId(), event.bizKey(),
                "随访计划已生成",
                "您的随访计划已生成，共 " + payload.taskCount() + " 次随访（"
                        + payload.templateName() + "），可在患者端查看时间轴。");
    }
}
