package com.yiliao.followup.remind;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiliao.common.mq.constant.Topics;
import com.yiliao.common.mq.event.BaseEvent;
import com.yiliao.followup.entity.FollowupTask;
import com.yiliao.followup.mapper.FollowupTaskMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 提醒派发（specs/flows/F2）：扫描到期提醒点 → 发即时 REMIND_DUE 消息。
 * 实现决策：rocketmq-spring 2.3.1 delayLevel 上限 2h，无法表达天级定时，
 * 默认采用"定时扫描派发"（需求分析报告 B1 方案一）；`remind.mode=mq-delay` 预留 5.x 定时消息升级。
 * 消费端幂等三保险不受生产端方式影响（bizKey=taskId:remindType）。
 */
@Service
public class RemindDispatchService {

    private static final Logger log = LoggerFactory.getLogger(RemindDispatchService.class);

    /** 单次扫描任务上限，防止大库拖垮分钟级 Job。 */
    static final int SCAN_BATCH = 500;

    private final FollowupTaskMapper taskMapper;
    private final RocketMQTemplate rocketMQTemplate;

    public RemindDispatchService(FollowupTaskMapper taskMapper, RocketMQTemplate rocketMQTemplate) {
        this.taskMapper = taskMapper;
        this.rocketMQTemplate = rocketMQTemplate;
    }

    /** XXL-Job 每分钟：今天应提醒的点 → 发消息 + remind_sent+1。返回派发条数。 */
    public int dispatchDueToday(LocalDate today) {
        List<FollowupTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<FollowupTask>()
                .in(FollowupTask::getStatus, FollowupTask.STATUS_PENDING, FollowupTask.STATUS_DUE_SOON,
                        FollowupTask.STATUS_OVERDUE)
                .between(FollowupTask::getPlanDate, today, today.plusDays(7))
                .last("LIMIT " + SCAN_BATCH));
        int sent = 0;
        for (FollowupTask task : tasks) {
            for (RemindPlanner.RemindPoint point : RemindPlanner.dueToday(task.getPlanDate(), today)) {
                if (sendRemind(task, point.remindType())) {
                    task.setRemindSent((task.getRemindSent() == null ? 0 : task.getRemindSent()) + 1);
                    taskMapper.updateById(task);
                    sent++;
                }
            }
        }
        if (sent > 0) {
            log.info("提醒派发完成 date={} sent={}", today, sent);
        }
        return sent;
    }

    /** 对账补投（每小时）：已到期但发送计数未覆盖的点，只补最近一个。 */
    public int reconcile(LocalDate today) {
        List<FollowupTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<FollowupTask>()
                .in(FollowupTask::getStatus, FollowupTask.STATUS_PENDING, FollowupTask.STATUS_DUE_SOON,
                        FollowupTask.STATUS_OVERDUE)
                .le(FollowupTask::getPlanDate, today.plusDays(7))
                .last("LIMIT " + SCAN_BATCH));
        int sent = 0;
        for (FollowupTask task : tasks) {
            int remindSent = task.getRemindSent() == null ? 0 : task.getRemindSent();
            List<RemindPlanner.RemindPoint> missed =
                    RemindPlanner.catchUpPoints(task.getPlanDate(), today, remindSent);
            if (!missed.isEmpty() && sendRemind(task, missed.get(0).remindType())) {
                task.setRemindSent(remindSent + 1);
                taskMapper.updateById(task);
                sent++;
            }
        }
        return sent;
    }

    /** 幂等由消费端 bizKey 兜底；发送失败返回 false 由对账补投。 */
    private boolean sendRemind(FollowupTask task, int remindType) {
        String bizKey = task.getId() + ":REMIND_T" + switch (remindType) {
            case 1 -> 7;
            case 2 -> 3;
            default -> 1;
        };
        try {
            BaseEvent<Map<String, Object>> event = BaseEvent.of(Topics.REMIND_DUE, bizKey, "yiliao-followup",
                    Map.of("taskId", task.getId(), "patientId", task.getPatientId(),
                            "planDate", task.getPlanDate().toString(),
                            "items", task.getItemsJson() == null ? "" : task.getItemsJson(),
                            "remindType", remindType));
            rocketMQTemplate.syncSend(Topics.REMIND_DUE,
                    MessageBuilder.withPayload(event).setHeader("KEYS", bizKey).build());
            return true;
        } catch (Exception e) {
            log.error("提醒消息发送失败 taskId={} bizKey={}", task.getId(), bizKey, e);
            return false;
        }
    }
}
