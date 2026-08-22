package com.yiliao.followup.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiliao.api.followup.dto.PlanGenerateRequest;
import com.yiliao.api.followup.dto.PlanTimelineDTO;
import com.yiliao.api.followup.dto.TaskDTO;
import com.yiliao.api.followup.dto.RuleInputDTO;
import com.yiliao.common.core.constant.YiliaoConstants;
import com.yiliao.common.core.exception.BizException;
import com.yiliao.followup.entity.DecisionRule;
import com.yiliao.followup.entity.FollowupPlan;
import com.yiliao.followup.entity.FollowupTask;
import com.yiliao.followup.entity.FollowupTemplate;
import com.yiliao.followup.engine.RuleEngineService;
import com.yiliao.followup.engine.TaskDatesPlanner;
import com.yiliao.followup.error.FollowupErrorCode;
import com.yiliao.followup.mapper.FollowupPlanMapper;
import com.yiliao.followup.mapper.FollowupTaskMapper;
import com.yiliao.followup.mapper.FollowupTemplateMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 计划生成（specs/modules/followup.md §4.2/§4.3）：
 * Redisson 锁防重 + active_flag 唯一索引兜底；规则引擎选模板；排期器铺任务。
 */
@Service
public class PlanService {

    private static final Logger log = LoggerFactory.getLogger(PlanService.class);

    private final FollowupPlanMapper planMapper;
    private final FollowupTaskMapper taskMapper;
    private final FollowupTemplateMapper templateMapper;
    private final RuleEngineService ruleEngineService;
    private final RedissonClient redissonClient;
    private final com.yiliao.followup.mq.PlanEventPublisher eventPublisher;

    public PlanService(FollowupPlanMapper planMapper, FollowupTaskMapper taskMapper,
                       FollowupTemplateMapper templateMapper, RuleEngineService ruleEngineService,
                       RedissonClient redissonClient,
                       com.yiliao.followup.mq.PlanEventPublisher eventPublisher) {
        this.planMapper = planMapper;
        this.taskMapper = taskMapper;
        this.templateMapper = templateMapper;
        this.ruleEngineService = ruleEngineService;
        this.redissonClient = redissonClient;
        this.eventPublisher = eventPublisher;
    }

    /** 生成计划（返回计划 id）。锁内三步：防重 → 匹配 → 铺任务。 */
    public Long generate(PlanGenerateRequest request) {
        String lockKey = YiliaoConstants.LOCK_PREFIX + "plan:" + request.patientId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(3, 30, TimeUnit.SECONDS)) {
                throw new BizException(FollowupErrorCode.PLAN_ALREADY_EXISTS.withMsg("计划生成并发冲突，请重试"));
            }
            return doGenerate(request);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(FollowupErrorCode.PLAN_ALREADY_EXISTS.withMsg("计划生成被中断"));
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional
    protected Long doGenerate(PlanGenerateRequest request) {
        Long active = planMapper.selectCount(new LambdaQueryWrapper<FollowupPlan>()
                .eq(FollowupPlan::getPatientId, request.patientId())
                .eq(FollowupPlan::getStatus, FollowupPlan.STATUS_ACTIVE));
        if (active != null && active > 0) {
            throw new BizException(FollowupErrorCode.PLAN_ALREADY_EXISTS);
        }

        RuleInputDTO input = request.input();
        DecisionRule rule = ruleEngineService.match(request.scene(), input);
        FollowupTemplate template = templateMapper.selectById(rule.getResultTemplateId());
        if (template == null) {
            throw new BizException(FollowupErrorCode.RULE_NOT_FOUND.withMsg("规则引用的模板不存在 ruleId=" + rule.getId()));
        }

        LocalDate start = request.startDate() != null ? LocalDate.parse(request.startDate()) : LocalDate.now();
        List<LocalDate> dates = TaskDatesPlanner.plan(start,
                nvl(rule.getFirstIntervalMonth()), nvl(rule.getRepeatIntervalMonth()), nvl(rule.getTotalYears()));

        FollowupPlan plan = new FollowupPlan();
        plan.setPatientId(request.patientId());
        plan.setTemplateId(template.getId());
        plan.setRuleId(rule.getId());
        plan.setStartDate(start);
        plan.setEndDate(dates.isEmpty() ? start : dates.get(dates.size() - 1));
        plan.setStatus(FollowupPlan.STATUS_ACTIVE);
        planMapper.insert(plan);

        int seq = 1;
        for (LocalDate date : dates) {
            FollowupTask task = new FollowupTask();
            task.setPlanId(plan.getId());
            task.setPatientId(request.patientId());
            task.setSeq(seq++);
            task.setPlanDate(date);
            task.setItemsJson(template.getItemsJson());
            task.setStatus(FollowupTask.STATUS_PENDING);
            taskMapper.insert(task);
        }
        log.info("计划生成 patientId={} planId={} ruleId={} tasks={}",
                request.patientId(), plan.getId(), rule.getId(), dates.size());
        eventPublisher.publishPlanCreated(request.patientId(), plan.getId(), dates.size(),
                template.getTemplateName());
        return plan.getId();
    }

    /** 重新生成（改规则即时生效 / 结节进展升级路径）：终止旧计划再生成，保留历史任务。 */
    public Long regenerate(Long patientId, Integer scene, RuleInputDTO input, String reason) {
        FollowupPlan active = requireActivePlan(patientId);
        active.setStatus(FollowupPlan.STATUS_TERMINATED);
        active.setAdjustReason(reason);
        planMapper.updateById(active);
        // 旧计划剩余未完成任务标记取消
        List<FollowupTask> remaining = taskMapper.selectList(new LambdaQueryWrapper<FollowupTask>()
                .eq(FollowupTask::getPlanId, active.getId())
                .in(FollowupTask::getStatus, FollowupTask.STATUS_PENDING, FollowupTask.STATUS_DUE_SOON,
                        FollowupTask.STATUS_OVERDUE));
        for (FollowupTask task : remaining) {
            task.setStatus(FollowupTask.STATUS_CANCELLED);
            taskMapper.updateById(task);
        }
        return generate(new PlanGenerateRequest(patientId, scene, input, LocalDate.now().toString()));
    }

    /** 患者时间轴（无进行中计划返回 null data）。 */
    public PlanTimelineDTO timeline(Long patientId) {
        FollowupPlan plan = planMapper.selectOne(new LambdaQueryWrapper<FollowupPlan>()
                .eq(FollowupPlan::getPatientId, patientId)
                .orderByDesc(FollowupPlan::getId)
                .last("LIMIT 1"));
        if (plan == null) {
            return null;
        }
        FollowupTemplate template = templateMapper.selectById(plan.getTemplateId());
        List<TaskDTO> tasks = taskMapper.selectList(new LambdaQueryWrapper<FollowupTask>()
                        .eq(FollowupTask::getPlanId, plan.getId())
                        .orderByAsc(FollowupTask::getSeq))
                .stream().map(PlanService::toTaskDTO).toList();
        return new PlanTimelineDTO(plan.getId(), plan.getPatientId(),
                template == null ? null : template.getTemplateName(),
                plan.getStartDate(), plan.getEndDate(), plan.getStatus(), plan.getAdjustReason(), tasks);
    }

    public FollowupPlan requireActivePlan(Long patientId) {
        FollowupPlan plan = planMapper.selectOne(new LambdaQueryWrapper<FollowupPlan>()
                .eq(FollowupPlan::getPatientId, patientId)
                .eq(FollowupPlan::getStatus, FollowupPlan.STATUS_ACTIVE));
        if (plan == null) {
            throw new BizException(FollowupErrorCode.PLAN_NOT_FOUND);
        }
        return plan;
    }

    static TaskDTO toTaskDTO(FollowupTask task) {
        return new TaskDTO(task.getId(), task.getPlanId(), task.getPatientId(), task.getSeq(),
                task.getPlanDate(), task.getItemsJson(), task.getStatus(), task.getDoneDate());
    }

    private static int nvl(Integer value) {
        return value == null ? 12 : value;
    }
}
