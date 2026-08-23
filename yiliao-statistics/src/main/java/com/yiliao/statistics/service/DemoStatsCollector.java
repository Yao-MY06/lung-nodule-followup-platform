package com.yiliao.statistics.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 演示数据源：构造自洽的模拟数据（毕设无真实数据，见 AGENTS「测试与验证」约定）。
 * P5b 替换为 Feign 拉取各服务真实数据，specs/modules/statistics.md §4.1。
 * 自洽性：结节两维度分布合计均等于在管人数 356；医生工作量为 5 位医生的当日计数。
 */
@Component
public class DemoStatsCollector implements StatsCollector {

    @Override
    public OverviewData collectOverview(LocalDate date) {
        return new OverviewData(356, 312, 23, 18);
    }

    @Override
    public MonthlyData collectMonthly(String month) {
        // 以月份做小幅扰动，让 6 个月趋势曲线有合理波动（同一月结果稳定，便于演示复现）
        int seed = Math.abs(month.hashCode());
        int due = 460 + seed % 40;          // 应随访 460~499
        int done = due - (30 + seed % 20);  // 完成率约 90%~94%
        int timely = done - (25 + seed % 15); // 及时率约再低 5~8 个百分点
        return new MonthlyData(due, done, timely);
    }

    @Override
    public List<NoduleDistItem> collectNoduleDist() {
        // 维度一 nodule_type：性质分布，合计 356 = 在管人数
        // 维度二 risk_level：风险分层，合计 356
        return List.of(
                new NoduleDistItem("nodule_type", "实性", 128),
                new NoduleDistItem("nodule_type", "部分实性", 96),
                new NoduleDistItem("nodule_type", "纯磨玻璃", 132),
                new NoduleDistItem("risk_level", "低危", 219),
                new NoduleDistItem("risk_level", "中危", 98),
                new NoduleDistItem("risk_level", "高危", 39));
    }

    @Override
    public List<DoctorWorkloadItem> collectDoctorWorkload(LocalDate date) {
        // 5 位医生当日工作量（姓名为虚构演示数据，非 PII）
        return List.of(
                new DoctorWorkloadItem(1L, "陈国栋", 4, 9, 2),
                new DoctorWorkloadItem(2L, "李慧敏", 3, 11, 1),
                new DoctorWorkloadItem(3L, "王建军", 5, 7, 3),
                new DoctorWorkloadItem(4L, "赵雪梅", 2, 12, 1),
                new DoctorWorkloadItem(5L, "孙立军", 4, 8, 2));
    }
}
