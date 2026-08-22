package com.yiliao.api.followup.dto;

import java.time.LocalDate;

/**
 * 随访任务（时间轴/工作台共用）。
 */
public record TaskDTO(
        Long id,
        Long planId,
        Long patientId,
        Integer seq,
        LocalDate planDate,
        String items,
        Integer status,
        LocalDate doneDate
) {
}
