package com.yiliao.followup.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yiliao.common.data.entity.BaseEntity;

import java.time.LocalDate;

/** followup_task（高频表；状态机见 specs/modules/followup.md §4.2）。 */
@TableName("followup_task")
public class FollowupTask extends BaseEntity {
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_DUE_SOON = 1;
    public static final int STATUS_OVERDUE = 2;
    public static final int STATUS_DONE = 3;
    public static final int STATUS_LOST = 4;
    public static final int STATUS_CANCELLED = 5;

    private Long planId;
    private Long patientId;
    private Integer seq;
    private LocalDate planDate;
    private String itemsJson;
    private Integer status = STATUS_PENDING;
    private Integer remindSent = 0;
    private LocalDate doneDate;
    private Long operatorId;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Integer getSeq() { return seq; }
    public void setSeq(Integer seq) { this.seq = seq; }
    public LocalDate getPlanDate() { return planDate; }
    public void setPlanDate(LocalDate planDate) { this.planDate = planDate; }
    public String getItemsJson() { return itemsJson; }
    public void setItemsJson(String itemsJson) { this.itemsJson = itemsJson; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getRemindSent() { return remindSent; }
    public void setRemindSent(Integer remindSent) { this.remindSent = remindSent; }
    public LocalDate getDoneDate() { return doneDate; }
    public void setDoneDate(LocalDate doneDate) { this.doneDate = doneDate; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
}
