package com.yiliao.patient.dto;

import com.yiliao.api.followup.dto.RuleInputDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 建档请求（specs/modules/patient.md §3 POST /archives）。
 * plan 非空时建档成功后自动调 followup 生成随访计划（失败不阻断建档，P4 升级 Seata）。
 */
public record CreateArchiveRequest(
        @NotBlank String name,
        Integer gender,
        LocalDate birthDate,
        String idCard,
        String phone,
        String address,
        String emergencyContact,
        String emergencyPhone,
        Long doctorId,
        @NotNull Integer sourceType,
        String remark,
        @Valid RiskFactorRequest riskFactor,
        DiagnosisRequest diagnosis,
        @Valid PlanRequest plan
) {

    public record RiskFactorRequest(
            BigDecimal smokingPackYear,
            Integer familyHistory,
            Integer occupationalExposure,
            Integer priorCancer,
            String comorbidity
    ) {
    }

    public record DiagnosisRequest(
            String tnmStage,
            String clinicalStage,
            String pathologyType,
            String geneResult,
            LocalDate surgeryDate,
            Integer adjuvantTherapy,
            LocalDate diagnoseDate
    ) {
    }

    public record PlanRequest(
            @NotNull Integer scene,
            @NotNull @Valid RuleInputDTO input
    ) {
    }
}
