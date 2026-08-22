package com.yiliao.followup.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yiliao.common.data.entity.BaseEntity;

import java.time.LocalDate;

/** followup_plan：active_flag 为生成列，实体不映射。 */
@TableName("followup_plan")
public class FollowupPlan extends BaseEntity {
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_DONE = 2;
    public static final int STATUS_TERMINATED = 3;

    private Long patientId;
    private Long templateId;
    private Long ruleId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer status;
    private String adjustReason;

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getAdjustReason() { return adjustReason; }
    public void setAdjustReason(String adjustReason) { this.adjustReason = adjustReason; }
}
