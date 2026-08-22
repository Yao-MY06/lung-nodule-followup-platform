package com.yiliao.followup.mq;

import com.yiliao.common.mq.constant.Topics;
import com.yiliao.common.mq.event.BaseEvent;
import com.yiliao.common.mq.event.PlanCreatedPayload;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * followup 事件发布（specs/global/30 §6）。发送失败仅告警不回滚：
 * PLAN_CREATED 是通知性事件，计划本身已在本地事务落库（提醒派发由扫描+对账兜底）。
 */
@Service
public class PlanEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PlanEventPublisher.class);

    private final RocketMQTemplate rocketMQTemplate;

    public PlanEventPublisher(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    public void publishPlanCreated(Long patientId, Long planId, Integer taskCount, String templateName) {
        try {
            BaseEvent<PlanCreatedPayload> event = BaseEvent.of(Topics.PLAN_CREATED,
                    "plan:" + planId, "yiliao-followup",
                    new PlanCreatedPayload(patientId, planId, taskCount, templateName));
            rocketMQTemplate.syncSend(Topics.PLAN_CREATED,
                    MessageBuilder.withPayload(event).build());
        } catch (Exception e) {
            log.warn("PLAN_CREATED 发送失败 planId={}（通知性事件，容忍丢失）", planId, e);
        }
    }
}
