package com.yiliao.nodule.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yiliao.common.data.entity.BaseEntity;

import java.time.LocalDate;

/** exam_report：raw_text 为 P4 AI 抽取输入；extract_status 0未抽取 1已抽取 2已确认。 */
@TableName("exam_report")
public class ExamReport extends BaseEntity {
    private Long patientId;
    private Integer reportType;
    private LocalDate examDate;
    private String orgName;
    private String rawText;
    private String structuredJson;
    private String conclusion;
    private String fileUrl;
    private Integer extractStatus = 0;

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Integer getReportType() { return reportType; }
    public void setReportType(Integer reportType) { this.reportType = reportType; }
    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }
    public String getOrgName() { return orgName; }
    public void setOrgName(String orgName) { this.orgName = orgName; }
    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }
    public String getStructuredJson() { return structuredJson; }
    public void setStructuredJson(String structuredJson) { this.structuredJson = structuredJson; }
    public String getConclusion() { return conclusion; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public Integer getExtractStatus() { return extractStatus; }
    public void setExtractStatus(Integer extractStatus) { this.extractStatus = extractStatus; }
}
