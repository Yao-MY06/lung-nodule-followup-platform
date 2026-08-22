package com.yiliao.patient.controller;

import com.yiliao.api.patient.PatientApi;
import com.yiliao.api.patient.dto.ArchiveDTO;
import com.yiliao.common.core.result.Result;
import com.yiliao.patient.entity.PatientArchive;
import com.yiliao.patient.service.ArchiveService;
import org.springframework.web.bind.annotation.RestController;

/**
 * PatientApi 契约实现（specs/global/30 §5）。
 * 注意：契约实现类不加类级 @RequestMapping——接口方法已带绝对路径，类级前缀会破坏映射。
 */
@RestController
public class PatientApiController implements PatientApi {

    private final ArchiveService archiveService;

    public PatientApiController(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @Override
    public Result<ArchiveDTO> getArchive(Long id) {
        PatientArchive archive = archiveService.requireArchive(id);
        return Result.ok(new ArchiveDTO(archive.getId(), archive.getPatientNo(), archive.getName(),
                archive.getStageLabel(), archive.getDoctorId(), archive.getSourceType()));
    }

    @Override
    public com.yiliao.common.core.result.Result<Boolean> hasRiskFactor(Long id) {
        archiveService.requireArchive(id);
        return Result.ok(archiveService.hasRiskFactor(id));
    }
}
