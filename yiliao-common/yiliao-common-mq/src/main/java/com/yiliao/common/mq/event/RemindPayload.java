package com.yiliao.common.mq.event;

import java.time.LocalDate;

/**
 * REMIND_DUE 载荷（specs/global/30 §6 契约）。
 */
public record RemindPayload(
        Long taskId,
        Long patientId,
        LocalDate planDate,
        String items,
        Integer remindType   // 1 T-7, 2 T-3, 3 T-1
) {
}
