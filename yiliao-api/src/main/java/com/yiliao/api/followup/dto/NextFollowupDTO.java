package com.yiliao.api.followup.dto;

import java.time.LocalDate;

/**
 * 下次随访任务（FollowupApi.nextFollowup 返回；无进行中计划返回 null data）。
 */
public record NextFollowupDTO(
        Long taskId,
        LocalDate planDate,
        String items,
        Integer seq
) {
}
