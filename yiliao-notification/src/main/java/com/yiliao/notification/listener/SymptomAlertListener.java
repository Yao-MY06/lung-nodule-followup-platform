package com.yiliao.notification.listener;

import com.yiliao.common.mq.constant.Topics;
import com.yiliao.common.mq.event.BaseEvent;
import com.yiliao.common.mq.event.SymptomAlertPayload;
import com.yiliao.notification.service.MessageService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SYMPTOM_ALERT 消费者（specs/flows/F4）：severity≥3 的症状推送主管医生站内信。
 * 医生 id 由 payload 补充（P3b：followup 发布时带上 doctorId，当前先发患者侧记录）。
 */
@Component
@RocketMQMessageListener(topic = Topics.SYMPTOM_ALERT, consumerGroup = "yiliao-notification-symptom")
public class SymptomAlertListener implements RocketMQListener<BaseEvent<SymptomAlertPayload>> {

    private static final Logger log = LoggerFactory.getLogger(SymptomAlertListener.class);

    private final MessageService messageService;

    public SymptomAlertListener(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void onMessage(BaseEvent<SymptomAlertPayload> event) {
        SymptomAlertPayload payload = event.payload();
        log.warn("症状预警事件 patientId={} symptom={} severity={}",
                payload.patientId(), payload.symptom(), payload.severity());
        // 医生侧接收人 id 解析依赖 PatientApi（P3b 补 doctorId 到载荷）；当前仅患者留痕
        messageService.sendInternal(payload.patientId(), event.bizKey(),
                "症状预警", "患者上报症状「" + payload.symptom() + "」达 " + payload.severity() + " 级，请及时关注。");
    }
}
