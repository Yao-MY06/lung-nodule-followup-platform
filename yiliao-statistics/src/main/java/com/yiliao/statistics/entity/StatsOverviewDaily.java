package com.yiliao.statistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yiliao.common.data.entity.BaseEntity;

import java.time.LocalDate;

/** stats_overview_daily：总览日快照（specs/modules/statistics.md §2）。 */
@TableName("stats_overview_daily")
public class StatsOverviewDaily extends BaseEntity {
    private LocalDate statDate;
    private Integer managingCount;
    private Integer activePlanCount;
    private Integer overdueTaskCount;
    private Integer alertCount;

    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate statDate) { this.statDate = statDate; }
    public Integer getManagingCount() { return managingCount; }
    public void setManagingCount(Integer managingCount) { this.managingCount = managingCount; }
    public Integer getActivePlanCount() { return activePlanCount; }
    public void setActivePlanCount(Integer activePlanCount) { this.activePlanCount = activePlanCount; }
    public Integer getOverdueTaskCount() { return overdueTaskCount; }
    public void setOverdueTaskCount(Integer overdueTaskCount) { this.overdueTaskCount = overdueTaskCount; }
    public Integer getAlertCount() { return alertCount; }
    public void setAlertCount(Integer alertCount) { this.alertCount = alertCount; }
}
