package com.yiliao.followup.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yiliao.common.data.entity.BaseEntity;

import java.time.LocalDateTime;

/** followup_record。 */
@TableName("followup_record")
public class FollowupRecord extends BaseEntity {
    private Long taskId;
    private Long patientId;
    private Integer followupType;
    private String content;
    private String resultSummary;
    private String nextAdvice;
    private Long operatorId;
    private LocalDateTime followupTime;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Integer getFollowupType() { return followupType; }
    public void setFollowupType(Integer followupType) { this.followupType = followupType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
    public String getNextAdvice() { return nextAdvice; }
    public void setNextAdvice(String nextAdvice) { this.nextAdvice = nextAdvice; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public LocalDateTime getFollowupTime() { return followupTime; }
    public void setFollowupTime(LocalDateTime followupTime) { this.followupTime = followupTime; }
}
