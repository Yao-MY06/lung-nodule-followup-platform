package com.yiliao.statistics.service;

import java.time.LocalDate;
import java.util.List;

/**
 * 统计取数接口：从各服务聚合原始计数（specs/modules/statistics.md §4.1）。
 * P5b 替换为 Feign 实现分页拉取 PatientApi/FollowupApi/NoduleApi/AuthApi；当前为演示实现（DemoStatsCollector）。
 */
public interface StatsCollector {

    /** 指定日期的总览计数（在管/进行中计划/逾期/预警）。 */
    OverviewData collectOverview(LocalDate date);

    /** 指定月（yyyy-MM）的随访完成计数。 */
    MonthlyData collectMonthly(String month);

    /** 结节类型/风险分布（快照全量，每轮整体重建）。 */
    List<NoduleDistItem> collectNoduleDist();

    /** 指定日期的医生工作量。 */
    List<DoctorWorkloadItem> collectDoctorWorkload(LocalDate date);

    record OverviewData(int managingCount, int activePlanCount, int overdueTaskCount, int alertCount) {
    }

    record MonthlyData(int dueCount, int doneCount, int timelyCount) {
    }

    record NoduleDistItem(String dimension, String label, int cnt) {
    }

    record DoctorWorkloadItem(Long doctorId, String doctorName, int archiveCount, int doneCount, int alertCount) {
    }
}
