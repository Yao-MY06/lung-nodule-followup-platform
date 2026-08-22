package com.yiliao.followup.service;

import com.yiliao.followup.entity.SymptomReport;
import com.yiliao.followup.mapper.SymptomReportMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * ePRO 症状上报（specs/modules/followup.md §3 POST /symptoms）。
 * severity≥3 置 alert_flag；YILIAO_SYMPTOM_ALERT 事件推送在 P3 接入（notification 就绪后）。
 */
@Service
public class SymptomService {

    private static final Logger log = LoggerFactory.getLogger(SymptomService.class);
    public static final int ALERT_SEVERITY = 3;

    private final SymptomReportMapper mapper;

    public SymptomService(SymptomReportMapper mapper) {
        this.mapper = mapper;
    }

    public Long report(Long patientId, String symptom, Integer severity, String description, Integer source) {
        SymptomReport report = new SymptomReport();
        report.setPatientId(patientId);
        report.setSymptom(symptom);
        report.setSeverity(severity);
        report.setDescription(description);
        report.setSource(source);
        report.setAlertFlag(severity != null && severity >= ALERT_SEVERITY ? 1 : 0);
        mapper.insert(report);
        if (report.getAlertFlag() == 1) {
            log.warn("症状预警 patientId={} symptom={} severity={}", patientId, symptom, severity);
            // P3: 发 YILIAO_SYMPTOM_ALERT → notification 推送主管医生
        }
        return report.getId();
    }
}
