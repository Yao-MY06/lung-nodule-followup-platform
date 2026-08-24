package com.yiliao.followup.controller;

import com.yiliao.common.core.result.Result;
import com.yiliao.followup.service.SymptomService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ePRO 症状上报（specs/modules/followup.md §3）。
 */
@RestController
@RequestMapping("/api/followup")
public class SymptomController {

    private final SymptomService symptomService;
    private final com.yiliao.followup.security.PatientDataGuard dataGuard;

    public SymptomController(SymptomService symptomService, com.yiliao.followup.security.PatientDataGuard dataGuard) {
        this.symptomService = symptomService;
        this.dataGuard = dataGuard;
    }

    @Operation(summary = "症状上报（severity≥3 触发预警标记，P3 接事件推送）")
    @PostMapping("/patients/{patientId}/symptoms")
    public Result<Long> report(@PathVariable Long patientId, @jakarta.validation.Valid @RequestBody SymptomRequest request) {
        dataGuard.requireOwnOrStaff(patientId);
        return Result.ok(symptomService.report(patientId, request.symptom(), request.severity(),
                request.description(), request.source()));
    }

    public record SymptomRequest(
            @NotBlank String symptom,
            @NotNull @Min(1) @Max(5) Integer severity,
            String description,
            Integer source
    ) {
    }
}
