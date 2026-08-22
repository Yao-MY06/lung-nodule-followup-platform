package com.yiliao.api.followup.dto;

/**
 * 计划生成请求（specs/modules/followup.md §3：POST /plans/generate）。
 * startDate 缺省为当天；仅支持从当天起排程。
 */
public record PlanGenerateRequest(
        Long patientId,
        Integer scene,
        RuleInputDTO input,
        String startDate
) {
}
