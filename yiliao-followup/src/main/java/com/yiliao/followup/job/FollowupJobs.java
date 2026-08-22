package com.yiliao.followup.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.yiliao.followup.remind.RemindDispatchService;
import com.yiliao.followup.scan.OverdueScanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * XXL-Job 任务入口（specs/flows/F2）。执行器由 XxlJobConfig 按需装配（默认关闭）。
 */
@Component
public class FollowupJobs {

    private static final Logger log = LoggerFactory.getLogger(FollowupJobs.class);

    private final RemindDispatchService remindDispatchService;
    private final OverdueScanService overdueScanService;

    public FollowupJobs(RemindDispatchService remindDispatchService, OverdueScanService overdueScanService) {
        this.remindDispatchService = remindDispatchService;
        this.overdueScanService = overdueScanService;
    }

    /** 每分钟：到期提醒派发（T-7/T-3/T-1）。 */
    @XxlJob("remindDispatchJob")
    public void remindDispatch() {
        int sent = remindDispatchService.dispatchDueToday(LocalDate.now());
        XxlJobHelper.handleSuccess("sent=" + sent);
    }

    /** 每小时：提醒对账补投（消息丢失兜底，幂等由消费端 bizKey 保证）。 */
    @XxlJob("remindReconcileJob")
    public void remindReconcile() {
        int sent = remindDispatchService.reconcile(LocalDate.now());
        XxlJobHelper.handleSuccess("reconcileSent=" + sent);
    }

    /** 每日 02:00：逾期/失访分片扫描（xxl-job 2.4.x：XxlJobHelper 取分片参数）。 */
    @XxlJob("overdueScanJob")
    public void overdueScan() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = Math.max(1, XxlJobHelper.getShardTotal());
        OverdueScanService.ScanResult result = overdueScanService.scan(LocalDate.now(),
                shardIndex, shardTotal);
        XxlJobHelper.handleSuccess("overdue=" + result.overdueCount() + " lost=" + result.lostCount());
        log.info("overdueScanJob shard={}/{} result={}", shardIndex, shardTotal, result);
    }
}
