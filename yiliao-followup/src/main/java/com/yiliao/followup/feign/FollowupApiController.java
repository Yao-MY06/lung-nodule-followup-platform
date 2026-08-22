package com.yiliao.followup.feign;

import com.yiliao.api.followup.FollowupApi;
import com.yiliao.api.followup.dto.NextFollowupDTO;
import com.yiliao.api.followup.dto.PlanGenerateRequest;
import com.yiliao.api.followup.dto.PlanTimelineDTO;
import com.yiliao.common.core.result.Result;
import com.yiliao.followup.service.PlanService;
import com.yiliao.followup.service.TaskService;
import org.springframework.web.bind.annotation.RestController;

/**
 * FollowupApi 契约实现（specs/global/30 §5）：@RestController 实现契约接口。
 */
@RestController
public class FollowupApiController implements FollowupApi {

    private final PlanService planService;
    private final TaskService taskService;
    private final com.yiliao.followup.service.SymptomService symptomService;

    public FollowupApiController(PlanService planService, TaskService taskService,
                                 com.yiliao.followup.service.SymptomService symptomService) {
        this.planService = planService;
        this.taskService = taskService;
        this.symptomService = symptomService;
    }

    @Override
    public Result<Long> generatePlan(PlanGenerateRequest request) {
        return Result.ok(planService.generate(request));
    }

    @Override
    public Result<PlanTimelineDTO> planTimeline(Long patientId) {
        return Result.ok(planService.timeline(patientId));
    }

    @Override
    public Result<NextFollowupDTO> nextFollowup(Long patientId) {
        return Result.ok(taskService.nextFollowup(patientId));
    }

    @Override
    public Result<Long> reportSymptom(Long patientId, SymptomRequest request) {
        return Result.ok(symptomService.report(patientId, request.symptom(), request.severity(),
                request.description(), request.source() == null ? 2 : request.source()));
    }
}
