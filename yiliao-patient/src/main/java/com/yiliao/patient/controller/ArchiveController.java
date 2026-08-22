package com.yiliao.patient.controller;

import com.yiliao.common.core.page.PageQuery;
import com.yiliao.common.core.page.PageResult;
import com.yiliao.common.core.result.Result;
import com.yiliao.patient.dto.CreateArchiveRequest;
import com.yiliao.patient.entity.PatientArchive;
import com.yiliao.patient.service.ArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 患者档案接口（specs/modules/patient.md §3）。契约实现见 PatientApiController。
 */
@RestController
@RequestMapping("/api/patient")
public class ArchiveController {

    private final ArchiveService archiveService;

    public ArchiveController(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @Operation(summary = "建档（可选自动生成随访计划；P4 升级 Seata）")
    @PostMapping("/archives")
    public Result<ArchiveService.CreateResult> create(@Valid @RequestBody CreateArchiveRequest request) {
        return Result.ok(archiveService.create(request));
    }

    @Operation(summary = "档案分页（按医生/阶段/关键字）")
    @GetMapping("/archives")
    public Result<PageResult<PatientArchive>> page(@RequestParam(required = false) Long doctorId,
                                                   @RequestParam(required = false) String stageLabel,
                                                   @RequestParam(required = false) String keyword,
                                                   @Valid PageQuery query) {
        return Result.ok(archiveService.page(doctorId, stageLabel, keyword, query));
    }

    @Operation(summary = "档案详情（PII 脱敏出参）")
    @GetMapping("/archives/{id}")
    public Result<ArchiveService.MaskedArchiveVO> detail(@PathVariable Long id) {
        return Result.ok(archiveService.detailMasked(id));
    }

    @Operation(summary = "变更阶段标签（状态机校验）")
    @PutMapping("/archives/{id}/stage")
    public Result<Void> changeStage(@PathVariable Long id, @RequestParam String stageLabel) {
        archiveService.changeStage(id, stageLabel);
        return Result.ok();
    }

    @Operation(summary = "变更主管医生")
    @PutMapping("/archives/{id}/doctor")
    public Result<Void> changeDoctor(@PathVariable Long id, @RequestParam Long doctorId) {
        archiveService.changeDoctor(id, doctorId);
        return Result.ok();
    }
}
