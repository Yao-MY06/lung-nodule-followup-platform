package com.yiliao.nodule.confirm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiliao.api.ai.dto.NoduleExtract;
import com.yiliao.api.followup.FollowupApi;
import com.yiliao.api.followup.dto.PlanGenerateRequest;
import com.yiliao.api.followup.dto.RuleInputDTO;
import com.yiliao.api.patient.PatientApi;
import com.yiliao.common.core.exception.BizException;
import com.yiliao.nodule.entity.ExamReport;
import com.yiliao.nodule.entity.Nodule;
import com.yiliao.nodule.entity.NoduleSnapshot;
import com.yiliao.nodule.error.NoduleErrorCode;
import com.yiliao.nodule.mapper.ExamReportMapper;
import com.yiliao.nodule.mapper.NoduleMapper;
import com.yiliao.nodule.mapper.NoduleSnapshotMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * AI 抽取确认入库（specs/modules/nodule.md §4.2，flows/F1 第 2~3 步）：
 * structured_json 草稿 → 医生确认 → 建结节 + 首条快照（extract_status 1→2）→ Feign 自动排期。
 * 安全底线：确认动作必须由医生发起，AI 只提供草稿（不变量：AI 只起草、人确认）。
 */
@Service
public class ConfirmService {

    private static final Logger log = LoggerFactory.getLogger(ConfirmService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Map<String, Integer> TYPE_CODE = Map.of(
            "SOLID", 1, "PART_SOLID", 2, "PURE_GG", 3);

    private final ExamReportMapper reportMapper;
    private final NoduleMapper noduleMapper;
    private final NoduleSnapshotMapper snapshotMapper;
    private final PatientApi patientApi;
    private final FollowupApi followupApi;

    public ConfirmService(ExamReportMapper reportMapper, NoduleMapper noduleMapper,
                          NoduleSnapshotMapper snapshotMapper, PatientApi patientApi,
                          FollowupApi followupApi) {
        this.reportMapper = reportMapper;
        this.noduleMapper = noduleMapper;
        this.snapshotMapper = snapshotMapper;
        this.patientApi = patientApi;
        this.followupApi = followupApi;
    }

    public record ConfirmResult(Long noduleId, Long snapshotId, Long planId, String planError) {
    }

    @Transactional
    public ConfirmResult confirm(Long reportId, boolean isNewNodule, Integer isNewFlag) {
        ExamReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BizException(NoduleErrorCode.REPORT_NOT_FOUND);
        }
        if (report.getExtractStatus() != null && report.getExtractStatus() == 2) {
            throw new BizException(NoduleErrorCode.ALREADY_CONFIRMED);
        }
        NoduleExtract extract;
        try {
            extract = MAPPER.readValue(report.getStructuredJson(), NoduleExtract.class);
        } catch (Exception e) {
            throw new BizException(NoduleErrorCode.STRUCTURED_JSON_INVALID.withMsg("抽取结果缺失或不合法，请先执行抽取"));
        }

        // 建结节（同一患者内按已有数编号 N1/N2…）
        Long count = noduleMapper.selectCount(new LambdaQueryWrapper<Nodule>()
                .eq(Nodule::getPatientId, report.getPatientId()));
        Nodule nodule = new Nodule();
        nodule.setPatientId(report.getPatientId());
        nodule.setNoduleNo("N" + (count + 1));
        nodule.setLocation(extract.location());
        nodule.setNoduleType(TYPE_CODE.getOrDefault(
                extract.noduleType() == null ? "" : extract.noduleType().toUpperCase(), 1));
        nodule.setStatus(1);
        nodule.setFirstFoundDate(report.getExamDate());
        noduleMapper.insert(nodule);

        // 首条基线快照
        NoduleSnapshot snapshot = new NoduleSnapshot();
        snapshot.setNoduleId(nodule.getId());
        snapshot.setReportId(report.getId());
        snapshot.setExamDate(report.getExamDate());
        snapshot.setMaxDiameterMm(extract.maxDiameterMm());
        snapshot.setSolidDiameterMm(extract.solidDiameterMm());
        snapshot.setSigns(extract.signs() == null ? null : String.join("、", extract.signs()));
        snapshot.setIsNew(isNewFlag == null ? 0 : isNewFlag);
        snapshotMapper.insert(snapshot);

        report.setExtractStatus(2);
        reportMapper.updateById(report);

        // 自动排期（F1 主线）：risk 取患者真实危险因素；失败不回滚确认，返回 planError 医生手动触发
        Long planId = null;
        String planError = null;
        try {
            Boolean risk = patientApi.hasRiskFactor(report.getPatientId()).data();
            RuleInputDTO input = new RuleInputDTO(nodule.getNoduleType(),
                    orDefault(extract.maxDiameterMm()), Boolean.TRUE.equals(risk), null, null, null);
            planId = followupApi.generatePlan(new PlanGenerateRequest(
                    report.getPatientId(), 1, input, LocalDate.now().toString())).data();
        } catch (Exception e) {
            planError = e.getMessage();
            log.warn("确认后排期失败 reportId={} msg={}", reportId, e.getMessage());
        }
        return new ConfirmResult(nodule.getId(), snapshot.getId(), planId, planError);
    }

    private static BigDecimal orDefault(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
