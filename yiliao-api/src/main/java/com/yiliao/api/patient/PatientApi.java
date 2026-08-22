package com.yiliao.api.patient;

import com.yiliao.api.patient.dto.ArchiveDTO;
import com.yiliao.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * patient 服务对外契约（specs/global/30 §5）。
 */
@FeignClient(name = "yiliao-patient", contextId = "patientApi")
public interface PatientApi {

    @GetMapping("/api/patient/internal/archives/{id}")
    Result<ArchiveDTO> getArchive(@PathVariable("id") Long id);

    /** 患者是否存在任一危险因素（随访规则引擎 risk 条件输入，F1 确认链路使用）。 */
    @GetMapping("/api/patient/internal/archives/{id}/risk-factor")
    Result<Boolean> hasRiskFactor(@PathVariable("id") Long id);
}
