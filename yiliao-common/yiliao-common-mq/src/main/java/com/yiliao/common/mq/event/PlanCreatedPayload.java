package com.yiliao.common.mq.event;

/**
 * PLAN_CREATED 载荷（specs/global/30 §6 契约）。
 */
public record PlanCreatedPayload(
        Long patientId,
        Long planId,
        Integer taskCount,
        String templateName
) {
}
