package com.yiliao.api.followup;

import com.yiliao.api.followup.dto.NextFollowupDTO;
import com.yiliao.api.followup.dto.PlanGenerateRequest;
import com.yiliao.api.followup.dto.PlanTimelineDTO;
import com.yiliao.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * followup 服务对外契约（specs/global/30 §5）。
 * generatePlan 供 patient 建档联动调用（P2 同步调用，P4 升级 Seata）。
 */
@FeignClient(name = "yiliao-followup", url = "${yiliao.feign.followup-url:http://localhost:8084}", contextId = "followupApi")
public interface FollowupApi {

    @PostMapping("/api/followup/internal/plans/generate")
    Result<Long> generatePlan(@RequestBody PlanGenerateRequest request);

    @GetMapping("/api/followup/internal/plans/{patientId}")
    Result<PlanTimelineDTO> planTimeline(@PathVariable("patientId") Long patientId);

    @GetMapping("/api/followup/internal/plans/{patientId}/next")
    Result<NextFollowupDTO> nextFollowup(@PathVariable("patientId") Long patientId);

    /** 症状上报（F4 AI 助手工具入口；source=2 AI助手）。 */
    @PostMapping("/api/followup/internal/patients/{patientId}/symptoms")
    Result<Long> reportSymptom(@PathVariable("patientId") Long patientId,
                               @RequestBody SymptomRequest request);

    record SymptomRequest(String symptom, Integer severity, String description, Integer source) {
    }
}
