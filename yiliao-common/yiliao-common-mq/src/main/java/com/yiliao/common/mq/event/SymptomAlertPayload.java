package com.yiliao.common.mq.event;

/**
 * SYMPTOM_ALERT 载荷（specs/global/30 §6 契约，severity>=3）。
 */
public record SymptomAlertPayload(
        Long patientId,
        String symptom,
        Integer severity,
        String description
) {
}
