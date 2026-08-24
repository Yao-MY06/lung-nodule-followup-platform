package com.yiliao.nodule.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yiliao.common.core.page.PageQuery;
import com.yiliao.common.core.page.PageResult;
import com.yiliao.common.core.result.Result;
import com.yiliao.common.data.page.PageResults;
import com.yiliao.nodule.compare.CompareService;
import com.yiliao.nodule.entity.ExamReport;
import com.yiliao.nodule.entity.Nodule;
import com.yiliao.nodule.entity.NoduleSnapshot;
import com.yiliao.nodule.mapper.ExamReportMapper;
import com.yiliao.nodule.mapper.NoduleMapper;
import com.yiliao.nodule.mapper.NoduleSnapshotMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 结节/快照/报告接口（specs/modules/nodule.md §3）。
 */
@RestController
@RequestMapping("/api/nodule")
public class NoduleController {

    private final NoduleMapper noduleMapper;
    private final NoduleSnapshotMapper snapshotMapper;
    private final ExamReportMapper reportMapper;
    private final CompareService compareService;
    private final com.yiliao.nodule.confirm.ConfirmService confirmService;
    private final com.yiliao.api.ai.AiApi aiApi;
    private final com.yiliao.nodule.security.PatientDataGuard dataGuard;

    public NoduleController(NoduleMapper noduleMapper, NoduleSnapshotMapper snapshotMapper,
                            ExamReportMapper reportMapper, CompareService compareService,
                            com.yiliao.nodule.confirm.ConfirmService confirmService,
                            com.yiliao.api.ai.AiApi aiApi,
                            com.yiliao.nodule.security.PatientDataGuard dataGuard) {
        this.noduleMapper = noduleMapper;
        this.snapshotMapper = snapshotMapper;
        this.reportMapper = reportMapper;
        this.compareService = compareService;
        this.confirmService = confirmService;
        this.aiApi = aiApi;
        this.dataGuard = dataGuard;
    }

    @Operation(summary = "登记结节（员工操作）")
    @PostMapping("/nodules")
    public Result<Long> register(@Valid @RequestBody Nodule nodule) {
        dataGuard.requireStaff();
        nodule.setId(null);
        noduleMapper.insert(nodule);
        return Result.ok(nodule.getId());
    }

    @Operation(summary = "录入复查快照（员工操作，触发对比分析）")
    @PostMapping("/snapshots")
    public Result<CompareService.CompareVO> snapshot(@Valid @RequestBody NoduleSnapshot snapshot) {
        dataGuard.requireStaff();
        snapshot.setId(null);
        snapshotMapper.insert(snapshot);
        return Result.ok(compareService.compare(snapshot.getNoduleId()));
    }

    @Operation(summary = "结节纵向趋势（直径/体积/密度序列）")
    @GetMapping("/nodules/{id}/trend")
    public Result<List<NoduleSnapshot>> trend(@PathVariable Long id) {
        dataGuard.requireStaff();
        return Result.ok(compareService.trend(id));
    }

    @Operation(summary = "最近两次快照对比（是否≥2mm、建议动作）")
    @GetMapping("/nodules/{id}/compare")
    public Result<CompareService.CompareVO> compare(@PathVariable Long id) {
        dataGuard.requireStaff();
        return Result.ok(compareService.compare(id));
    }

    @Operation(summary = "患者结节列表（患者仅可查本人）")
    @GetMapping("/patients/{patientId}/nodules")
    public Result<List<Nodule>> byPatient(@PathVariable Long patientId) {
        dataGuard.requireOwnOrStaff(patientId);
        return Result.ok(noduleMapper.selectList(new LambdaQueryWrapper<Nodule>()
                .eq(Nodule::getPatientId, patientId)));
    }

    @Operation(summary = "报告录入（员工操作，AI 抽取 P4）")
    @PostMapping("/exam/reports")
    public Result<Long> createReport(@Valid @RequestBody ReportCreateRequest request) {
        dataGuard.requireStaff();
        ExamReport report = new ExamReport();
        report.setPatientId(request.patientId());
        report.setReportType(request.reportType());
        report.setExamDate(request.examDate());
        report.setOrgName(request.orgName());
        report.setRawText(request.rawText());
        report.setConclusion(request.conclusion());
        report.setExtractStatus(0);
        reportMapper.insert(report);
        return Result.ok(report.getId());
    }

    @Operation(summary = "报告列表（患者仅可查本人）")
    @GetMapping("/exam/reports")
    public Result<PageResult<ExamReport>> reports(@RequestParam Long patientId,
                                                  @RequestParam(required = false) Integer reportType,
                                                  @Valid PageQuery query) {
        dataGuard.requireOwnOrStaff(patientId);
        return Result.ok(PageResults.of(reportMapper.selectPage(
                Page.of(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<ExamReport>()
                        .eq(ExamReport::getPatientId, patientId)
                        .eq(reportType != null, ExamReport::getReportType, reportType)
                        .orderByDesc(ExamReport::getExamDate))));
    }

    // ── F1 确认链路（specs/flows/F1 第 2~3 步；依赖见类头构造器） ──

    @Operation(summary = "触发 AI 抽取（员工操作，草稿回填 structured_json，不入库）")
    @PostMapping("/exam/reports/{id}/extract")
    public Result<com.yiliao.api.ai.dto.NoduleExtract> extract(@PathVariable Long id) {
        dataGuard.requireStaff();
        ExamReport report = reportMapper.selectById(id);
        if (report == null) {
            return Result.fail(com.yiliao.nodule.error.NoduleErrorCode.REPORT_NOT_FOUND);
        }
        com.yiliao.api.ai.dto.NoduleExtract extract = aiApi
                .extractReport(new com.yiliao.api.ai.AiApi.ExtractRequest(report.getRawText())).data();
        report.setStructuredJson(toJson(extract));
        report.setExtractStatus(1);
        reportMapper.updateById(report);
        return Result.ok(extract);
    }

    @Operation(summary = "医生确认 AI 抽取结果入库（建结节+快照+自动排期）")
    @PostMapping("/exam/reports/{id}/confirm")
    public Result<com.yiliao.nodule.confirm.ConfirmService.ConfirmResult> confirm(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean newNodule,
            @RequestParam(required = false) Integer isNewFlag) {
        dataGuard.requireStaff();
        return Result.ok(confirmService.confirm(id, newNodule, isNewFlag));
    }

    private static String toJson(Object value) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("序列化失败", e);
        }
    }

    public record ReportCreateRequest(
            @NotNull Long patientId,
            @NotNull Integer reportType,
            @NotNull java.time.LocalDate examDate,
            String orgName,
            @NotEmpty String rawText,
            String conclusion
    ) {
    }
}
