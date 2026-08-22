package com.yiliao.api.followup.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 患者计划时间轴（FollowupApi.planTimeline 返回）。
 */
public record PlanTimelineDTO(
        Long planId,
        Long patientId,
        String templateName,
        LocalDate startDate,
        LocalDate endDate,
        Integer status,
        String adjustReason,
        List<TaskDTO> tasks
) {
}
