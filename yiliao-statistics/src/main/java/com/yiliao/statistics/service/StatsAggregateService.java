package com.yiliao.statistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiliao.statistics.entity.StatsDoctorWorkload;
import com.yiliao.statistics.entity.StatsFollowupMonthly;
import com.yiliao.statistics.entity.StatsNoduleDist;
import com.yiliao.statistics.entity.StatsOverviewDaily;
import com.yiliao.statistics.mapper.StatsDoctorWorkloadMapper;
import com.yiliao.statistics.mapper.StatsFollowupMonthlyMapper;
import com.yiliao.statistics.mapper.StatsNoduleDistMapper;
import com.yiliao.statistics.mapper.StatsOverviewDailyMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * 快照聚合与查询（specs/modules/statistics.md §4/§5）。
 * 刷新走「物理删旧 + 插新」同一事务：上游取数失败抛异常即回滚，保留上一轮快照（specs §5）；
 * 查询全部只读快照表，空表返回空集合/零值对象，不报错（specs §5）。
 */
@Service
public class StatsAggregateService {

    private static final Logger log = LoggerFactory.getLogger(StatsAggregateService.class);
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    /** followup-rate 查询月数上限（前端趋势图最多展示 24 个月）。 */
    private static final int MAX_MONTHS = 24;

    private final StatsOverviewDailyMapper overviewMapper;
    private final StatsFollowupMonthlyMapper monthlyMapper;
    private final StatsNoduleDistMapper distMapper;
    private final StatsDoctorWorkloadMapper workloadMapper;
    private final StatsCollector collector;

    public StatsAggregateService(StatsOverviewDailyMapper overviewMapper,
                                 StatsFollowupMonthlyMapper monthlyMapper,
                                 StatsNoduleDistMapper distMapper,
                                 StatsDoctorWorkloadMapper workloadMapper,
                                 StatsCollector collector) {
        this.overviewMapper = overviewMapper;
        this.monthlyMapper = monthlyMapper;
        this.distMapper = distMapper;
        this.workloadMapper = workloadMapper;
        this.collector = collector;
    }

    /** Job 入口：刷新当日/当月快照。 */
    public void refreshAll() {
        refreshAll(LocalDate.now());
    }

    /** 清空并重建指定日期的日快照与指定当月的月快照、全量重建分布快照。 */
    @Transactional
    public void refreshAll(LocalDate date) {
        String month = date.format(MONTH_FMT);

        overviewMapper.physicalDeleteByDate(date);
        StatsCollector.OverviewData overview = collector.collectOverview(date);
        StatsOverviewDaily daily = new StatsOverviewDaily();
        daily.setStatDate(date);
        daily.setManagingCount(overview.managingCount());
        daily.setActivePlanCount(overview.activePlanCount());
        daily.setOverdueTaskCount(overview.overdueTaskCount());
        daily.setAlertCount(overview.alertCount());
        overviewMapper.insert(daily);

        monthlyMapper.physicalDeleteByMonth(month);
        StatsCollector.MonthlyData monthly = collector.collectMonthly(month);
        StatsFollowupMonthly monthlyRow = new StatsFollowupMonthly();
        monthlyRow.setStatMonth(month);
        monthlyRow.setDueCount(monthly.dueCount());
        monthlyRow.setDoneCount(monthly.doneCount());
        monthlyRow.setTimelyCount(monthly.timelyCount());
        monthlyMapper.insert(monthlyRow);

        distMapper.physicalDeleteAll();
        List<StatsNoduleDist> dists = collector.collectNoduleDist().stream().map(item -> {
            StatsNoduleDist dist = new StatsNoduleDist();
            dist.setDimension(item.dimension());
            dist.setLabel(item.label());
            dist.setCnt(item.cnt());
            return dist;
        }).toList();
        if (!dists.isEmpty()) {
            distMapper.insert(dists);
        }

        workloadMapper.physicalDeleteByDate(date);
        List<StatsDoctorWorkload> workloads = collector.collectDoctorWorkload(date).stream().map(item -> {
            StatsDoctorWorkload workload = new StatsDoctorWorkload();
            workload.setStatDate(date);
            workload.setDoctorId(item.doctorId());
            workload.setDoctorName(item.doctorName());
            workload.setArchiveCount(item.archiveCount());
            workload.setDoneCount(item.doneCount());
            workload.setAlertCount(item.alertCount());
            return workload;
        }).toList();
        if (!workloads.isEmpty()) {
            workloadMapper.insert(workloads);
        }

        log.info("统计快照刷新完成 date={} month={} distRows={} workloadRows={}",
                date, month, dists.size(), workloads.size());
    }

    /** 总览：最新一个快照日；空表返回零值对象（specs §5）。 */
    public StatsOverviewDaily overview() {
        StatsOverviewDaily latest = overviewMapper.selectOne(new LambdaQueryWrapper<StatsOverviewDaily>()
                .orderByDesc(StatsOverviewDaily::getStatDate)
                .last("LIMIT 1"));
        if (latest != null) {
            return latest;
        }
        StatsOverviewDaily empty = new StatsOverviewDaily();
        empty.setManagingCount(0);
        empty.setActivePlanCount(0);
        empty.setOverdueTaskCount(0);
        empty.setAlertCount(0);
        return empty;
    }

    /** 结节类型/风险分布（快照全量，dimension 升序）；空表返回空集合。 */
    public List<StatsNoduleDist> noduleDistribution() {
        return distMapper.selectList(new LambdaQueryWrapper<StatsNoduleDist>()
                .orderByAsc(StatsNoduleDist::getDimension));
    }

    /** 最近 months 个月随访月快照（yyyy-MM 定长格式，字典序=时间序），升序输出；空表返回空集合。 */
    public List<StatsFollowupMonthly> followupRate(int months) {
        int limit = Math.min(Math.max(months, 1), MAX_MONTHS);
        String earliest = LocalDate.now().withDayOfMonth(1).minusMonths(limit - 1L).format(MONTH_FMT);
        return monthlyMapper.selectList(new LambdaQueryWrapper<StatsFollowupMonthly>()
                        .ge(StatsFollowupMonthly::getStatMonth, earliest))
                .stream()
                .sorted(Comparator.comparing(StatsFollowupMonthly::getStatMonth))
                .toList();
    }

    /** 最新快照日的医生工作量（按完成任务数降序）；空表返回空集合。 */
    public List<StatsDoctorWorkload> doctorWorkload() {
        StatsDoctorWorkload latest = workloadMapper.selectOne(new LambdaQueryWrapper<StatsDoctorWorkload>()
                .orderByDesc(StatsDoctorWorkload::getStatDate)
                .last("LIMIT 1"));
        if (latest == null) {
            return List.of();
        }
        return workloadMapper.selectList(new LambdaQueryWrapper<StatsDoctorWorkload>()
                .eq(StatsDoctorWorkload::getStatDate, latest.getStatDate())
                .orderByDesc(StatsDoctorWorkload::getDoneCount));
    }
}
