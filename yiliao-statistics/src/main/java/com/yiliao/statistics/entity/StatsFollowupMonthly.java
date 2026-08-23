package com.yiliao.statistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yiliao.common.data.entity.BaseEntity;

/** stats_followup_monthly：随访完成率月快照（specs/modules/statistics.md §2）。 */
@TableName("stats_followup_monthly")
public class StatsFollowupMonthly extends BaseEntity {
    /** 统计月，格式 yyyy-MM（定长格式，字典序=时间序）。 */
    private String statMonth;
    private Integer dueCount;
    private Integer doneCount;
    private Integer timelyCount;

    public String getStatMonth() { return statMonth; }
    public void setStatMonth(String statMonth) { this.statMonth = statMonth; }
    public Integer getDueCount() { return dueCount; }
    public void setDueCount(Integer dueCount) { this.dueCount = dueCount; }
    public Integer getDoneCount() { return doneCount; }
    public void setDoneCount(Integer doneCount) { this.doneCount = doneCount; }
    public Integer getTimelyCount() { return timelyCount; }
    public void setTimelyCount(Integer timelyCount) { this.timelyCount = timelyCount; }
}
