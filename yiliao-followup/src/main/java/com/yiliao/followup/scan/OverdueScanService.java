package com.yiliao.followup.scan;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiliao.followup.entity.FollowupTask;
import com.yiliao.followup.mapper.FollowupTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 逾期/失访扫描（specs/flows/F2 链路二）：每日 02:00 XXL-Job 分片调用。
 * 逾期/失访的医生通知与档案阶段同步在 P3b 补 NotifyApi 调用与 PatientApi 阶段契约（当前仅置任务状态+日志）。
 */
@Service
public class OverdueScanService {

    private static final Logger log = LoggerFactory.getLogger(OverdueScanService.class);

    static final int SCAN_BATCH = 1000;

    private final FollowupTaskMapper taskMapper;

    public OverdueScanService(FollowupTaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    /** 分片扫描：shardIndex/shardTotal 按 patientId 取模（specs/global/30 XXL-Job 约定）。 */
    public ScanResult scan(LocalDate today, int shardIndex, int shardTotal) {
        int overdue = 0;
        int lost = 0;
        List<FollowupTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<FollowupTask>()
                .in(FollowupTask::getStatus, FollowupTask.STATUS_PENDING, FollowupTask.STATUS_DUE_SOON,
                        FollowupTask.STATUS_OVERDUE)
                .lt(FollowupTask::getPlanDate, today)
                .last("LIMIT " + SCAN_BATCH));
        for (FollowupTask task : tasks) {
            if (task.getPatientId() % shardTotal != shardIndex) {
                continue;
            }
            OverdueClassifier.Outcome outcome = OverdueClassifier.classify(task.getStatus(),
                    task.getPlanDate(), today);
            switch (outcome) {
                case MARK_OVERDUE -> {
                    if (task.getStatus() != FollowupTask.STATUS_OVERDUE) {
                        task.setStatus(FollowupTask.STATUS_OVERDUE);
                        taskMapper.updateById(task);
                        log.warn("任务逾期 taskId={} patientId={} planDate={}",
                                task.getId(), task.getPatientId(), task.getPlanDate());
                        overdue++;
                    }
                }
                case MARK_LOST -> {
                    task.setStatus(FollowupTask.STATUS_LOST);
                    taskMapper.updateById(task);
                    log.warn("患者疑似失访 patientId={} taskId={}（阶段同步 P3b）",
                            task.getPatientId(), task.getId());
                    lost++;
                }
                default -> { }
            }
        }
        return new ScanResult(overdue, lost);
    }

    public record ScanResult(int overdueCount, int lostCount) {
    }
}
