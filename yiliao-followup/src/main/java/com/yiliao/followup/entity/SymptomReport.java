package com.yiliao.followup.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** symptom_report：追加型数据，无逻辑删除（DDL 即如此）。 */
@TableName("symptom_report")
public class SymptomReport {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long patientId;
    private String symptom;
    private Integer severity;
    private String description;
    private Integer source;
    private Integer alertFlag = 0;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getSymptom() { return symptom; }
    public void setSymptom(String symptom) { this.symptom = symptom; }
    public Integer getSeverity() { return severity; }
    public void setSeverity(Integer severity) { this.severity = severity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getSource() { return source; }
    public void setSource(Integer source) { this.source = source; }
    public Integer getAlertFlag() { return alertFlag; }
    public void setAlertFlag(Integer alertFlag) { this.alertFlag = alertFlag; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
