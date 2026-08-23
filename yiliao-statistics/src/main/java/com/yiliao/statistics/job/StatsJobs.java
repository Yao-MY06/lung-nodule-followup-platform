package com.yiliao.statistics.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.yiliao.statistics.service.StatsAggregateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * XXL-Job 任务入口（specs/modules/statistics.md §4.1，每小时预聚合）。
 * 执行器由 XxlJobConfig 按需装配（默认关闭）；取数失败抛异常 → 事务回滚保留上一轮快照（specs §5）。
 */
@Component
public class StatsJobs {

    private static final Logger log = LoggerFactory.getLogger(StatsJobs.class);

    private final StatsAggregateService statsAggregateService;

    public StatsJobs(StatsAggregateService statsAggregateService) {
        this.statsAggregateService = statsAggregateService;
    }

    /** 每小时：拉取各服务数据重建当日/当月快照（驾驶舱只读快照）。 */
    @XxlJob("statsAggregateJob")
    public void aggregate() {
        statsAggregateService.refreshAll();
        XxlJobHelper.handleSuccess("refreshAll ok");
        log.info("statsAggregateJob 统计快照刷新成功");
    }
}
