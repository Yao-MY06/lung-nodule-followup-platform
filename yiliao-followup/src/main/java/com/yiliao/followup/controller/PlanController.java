package com.yiliao.followup.controller;

import com.yiliao.api.followup.dto.PlanGenerateRequest;
import com.yiliao.api.followup.dto.PlanTimelineDTO;
import com.yiliao.api.followup.dto.RuleInputDTO;
import com.yiliao.common.core.result.Result;
import com.yiliao.followup.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 随访计划接口（specs/modules/followup.md §3）。
 */
@RestController
@RequestMapping("/api/followup")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @Operation(summary = "手动触发计划生成（规则匹配+防重）")
    @PostMapping("/plans/generate")
    public Result<Long> generate(@Valid @RequestBody PlanGenerateRequest request) {
        return Result.ok(planService.generate(request));
    }

    @Operation(summary = "重新生成（终止旧计划；改规则/结节进展升级路径）")
    @PostMapping("/plans/{patientId}/regenerate")
    public Result<Long> regenerate(@PathVariable Long patientId, @RequestParam Integer scene,
                                   @RequestBody RuleInputDTO input, @RequestParam String reason) {
        return Result.ok(planService.regenerate(patientId, scene, input, reason));
    }

    @Operation(summary = "患者随访计划+任务时间轴")
    @GetMapping("/plans/{patientId}")
    public Result<PlanTimelineDTO> timeline(@PathVariable Long patientId) {
        return Result.ok(planService.timeline(patientId));
    }
}
