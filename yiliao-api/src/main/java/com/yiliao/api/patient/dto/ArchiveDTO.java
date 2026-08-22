package com.yiliao.api.patient.dto;

/**
 * 患者档案简要信息（跨服务展示用；PII 字段由 patient 侧脱敏后返回）。
 */
public record ArchiveDTO(
        Long id,
        String patientNo,
        String name,
        String stageLabel,
        Long doctorId,
        Integer sourceType
) {
}
