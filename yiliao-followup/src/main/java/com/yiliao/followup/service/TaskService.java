package com.yiliao.followup.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yiliao.api.followup.dto.NextFollowupDTO;
import com.yiliao.common.core.exception.BizException;
import com.yiliao.common.core.page.PageQuery;
import com.yiliao.common.core.page.PageResult;
import com.yiliao.common.data.page.PageResults;
import com.yiliao.followup.entity.FollowupPlan;
import com.yiliao.followup.entity.FollowupRecord;
import com.yiliao.followup.entity.FollowupTask;
import com.yiliao.followup.error.FollowupErrorCode;
import com.yiliao.followup.mapper.FollowupPlanMapper;
import com.yiliao.followup.mapper.FollowupRecordMapper;
import com.yiliao.followup.mapper.FollowupTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 任务执行（specs/modules/followup.md §3/§4）：工作台、调整、完成（写记录→状态流转→计划收尾）。
 */
@Service
public class TaskService {

    private final FollowupTaskMapper taskMapper;
    private final FollowupRecordMapper recordMapper;
    private final FollowupPlanMapper planMapper;

    public TaskService(FollowupTaskMapper taskMapper, FollowupRecordMapper recordMapper,
                       FollowupPlanMapper planMapper) {
        this.taskMapper = taskMapper;
        this.recordMapper = recordMapper;
        this.planMapper = planMapper;
    }

    /** 工作台：按状态筛选，plan_date 升序（specs/modules/followup.md §3）。 */
    public PageResult<FollowupTask> workbench(Integer status, PageQuery query) {
        LambdaQueryWrapper<FollowupTask> wrapper = new LambdaQueryWrapper<FollowupTask>()
                .eq(status != null, FollowupTask::getStatus, status)
                .orderByAsc(FollowupTask::getPlanDate);
        return PageResults.of(taskMapper.selectPage(Page.of(query.getPage(), query.getSize()), wrapper));
    }

    /** 下次随访：最早的未完成任务。 */
    public NextFollowupDTO nextFollowup(Long patientId) {
        FollowupTask task = taskMapper.selectOne(new LambdaQueryWrapper<FollowupTask>()
                .eq(FollowupTask::getPatientId, patientId)
                .in(FollowupTask::getStatus, FollowupTask.STATUS_PENDING, FollowupTask.STATUS_DUE_SOON,
                        FollowupTask.STATUS_OVERDUE)
                .orderByAsc(FollowupTask::getPlanDate)
                .last("LIMIT 1"));
        if (task == null) {
            return null;
        }
        return new NextFollowupDTO(task.getId(), task.getPlanDate(), task.getItemsJson(), task.getSeq());
    }

    /** 调整单次任务（日期/项目）。 */
    public void adjust(Long taskId, LocalDate newPlanDate, String itemsJson) {
        FollowupTask task = requireTask(taskId);
        if (task.getStatus() == FollowupTask.STATUS_DONE) {
            throw new BizException(FollowupErrorCode.TASK_STATUS_ILLEGAL.withMsg("已完成任务不可调整"));
        }
        if (newPlanDate != null) {
            task.setPlanDate(newPlanDate);
        }
        if (itemsJson != null) {
            task.setItemsJson(itemsJson);
        }
        taskMapper.updateById(task);
    }

    /** 提交随访记录：任务→完成；计划内无剩余任务则计划收尾（specs/modules/followup.md §4.6）。 */
    @Transactional
    public void completeTask(Long taskId, Integer followupType, String content, String resultSummary,
                             String nextAdvice, Long operatorId) {
        FollowupTask task = requireTask(taskId);
        if (task.getStatus() == FollowupTask.STATUS_DONE) {
            throw new BizException(FollowupErrorCode.TASK_STATUS_ILLEGAL.withMsg("任务已完成，禁止重复完成"));
        }
        if (task.getStatus() == FollowupTask.STATUS_CANCELLED) {
            throw new BizException(FollowupErrorCode.TASK_STATUS_ILLEGAL.withMsg("任务已取消"));
        }
        task.setStatus(FollowupTask.STATUS_DONE);
        task.setDoneDate(LocalDate.now());
        task.setOperatorId(operatorId);
        taskMapper.updateById(task);

        FollowupRecord record = new FollowupRecord();
        record.setTaskId(task.getId());
        record.setPatientId(task.getPatientId());
        record.setFollowupType(followupType);
        record.setContent(content);
        record.setResultSummary(resultSummary);
        record.setNextAdvice(nextAdvice);
        record.setOperatorId(operatorId);
        record.setFollowupTime(LocalDateTime.now());
        recordMapper.insert(record);

        Long remaining = taskMapper.selectCount(new LambdaQueryWrapper<FollowupTask>()
                .eq(FollowupTask::getPlanId, task.getPlanId())
                .in(FollowupTask::getStatus, FollowupTask.STATUS_PENDING, FollowupTask.STATUS_DUE_SOON,
                        FollowupTask.STATUS_OVERDUE));
        if (remaining != null && remaining == 0) {
            FollowupPlan plan = planMapper.selectById(task.getPlanId());
            if (plan != null && plan.getStatus() != null && plan.getStatus() == FollowupPlan.STATUS_ACTIVE) {
                plan.setStatus(FollowupPlan.STATUS_DONE);
                planMapper.updateById(plan);
            }
        }
    }

    private FollowupTask requireTask(Long taskId) {
        FollowupTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(FollowupErrorCode.TASK_NOT_FOUND);
        }
        return task;
    }
}
