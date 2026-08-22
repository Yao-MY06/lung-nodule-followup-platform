package com.yiliao.followup.service;

import com.yiliao.api.followup.dto.PlanGenerateRequest;
import com.yiliao.api.followup.dto.RuleInputDTO;
import com.yiliao.followup.engine.RuleEngineService;
import com.yiliao.followup.entity.DecisionRule;
import com.yiliao.followup.entity.FollowupTemplate;
import com.yiliao.followup.error.FollowupErrorCode;
import com.yiliao.followup.mapper.DecisionRuleMapper;
import com.yiliao.followup.mapper.FollowupPlanMapper;
import com.yiliao.followup.mapper.FollowupTaskMapper;
import com.yiliao.followup.mapper.FollowupTemplateMapper;
import com.yiliao.common.core.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private FollowupPlanMapper planMapper;
    @Mock
    private FollowupTaskMapper taskMapper;
    @Mock
    private FollowupTemplateMapper templateMapper;
    @Mock
    private DecisionRuleMapper ruleMapper;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;
    @Mock
    private com.yiliao.followup.mq.PlanEventPublisher eventPublisher;

    private PlanService planService;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(redissonClient.getLock(anyString())).thenReturn(lock);
        lenient().when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        lenient().when(lock.isHeldByCurrentThread()).thenReturn(true);
        planService = new PlanService(planMapper, taskMapper, templateMapper,
                new RuleEngineService(ruleMapper), redissonClient, eventPublisher);
    }

    private DecisionRule ggnRule() {
        DecisionRule rule = new DecisionRule();
        rule.setId(7L);
        rule.setScene(1);
        rule.setCondJson("{\"nodule_type\":3,\"max_dia\":\"<=5\"}");
        rule.setResultTemplateId(2L);
        rule.setFirstIntervalMonth(6);
        rule.setRepeatIntervalMonth(12);
        rule.setTotalYears(5);
        rule.setPriority(40);
        rule.setStatus(1);
        return rule;
    }

    private FollowupTemplate template() {
        FollowupTemplate template = new FollowupTemplate();
        template.setId(2L);
        template.setItemsJson("{\"items\":[\"胸部CT\"]}");
        template.setStatus(1);
        return template;
    }

    @Test
    void generateCreatesPlanAndTasks() {
        when(planMapper.selectCount(any())).thenReturn(0L);
        when(ruleMapper.selectList(any())).thenReturn(List.of(ggnRule()));
        when(templateMapper.selectById(2L)).thenReturn(template());
        // 模拟数据库自增主键回填
        when(planMapper.insert(any(com.yiliao.followup.entity.FollowupPlan.class)))
                .thenAnswer(invocation -> {
                    invocation.getArgument(0, com.yiliao.followup.entity.FollowupPlan.class).setId(1L);
                    return 1;
                });

        Long planId = planService.generate(new PlanGenerateRequest(
                1001L, 1, new RuleInputDTO(3, new BigDecimal("4.0"), false, null, null, null),
                "2026-08-22"));

        assertEquals(1L, planId);
        // 首次 6 个月 + 之后 12 个月 × 5 年 → 5 个任务
        ArgumentCaptor<Object> ignored = ArgumentCaptor.forClass(Object.class);
        verify(planMapper).insert(any(com.yiliao.followup.entity.FollowupPlan.class));
        verify(taskMapper, org.mockito.Mockito.times(5)).insert(any(com.yiliao.followup.entity.FollowupTask.class));
        verify(lock).unlock();
    }

    @Test
    void duplicateActivePlanRejected() {
        when(planMapper.selectCount(any())).thenReturn(1L);

        BizException e = assertThrows(BizException.class, () -> planService.generate(new PlanGenerateRequest(
                1001L, 1, new RuleInputDTO(3, new BigDecimal("4.0"), false, null, null, null), null)));
        assertEquals(FollowupErrorCode.PLAN_ALREADY_EXISTS.getCode(), e.getCode());
        verify(taskMapper, org.mockito.Mockito.never()).insert(any(com.yiliao.followup.entity.FollowupTask.class));
    }

    @Test
    void noRuleMatchRejectsWith22002() {
        when(planMapper.selectCount(any())).thenReturn(0L);
        when(ruleMapper.selectList(any())).thenReturn(List.of());

        BizException e = assertThrows(BizException.class, () -> planService.generate(new PlanGenerateRequest(
                1001L, 1, new RuleInputDTO(1, new BigDecimal("30.0"), false, null, null, null), null)));
        assertEquals(FollowupErrorCode.RULE_NOT_FOUND.getCode(), e.getCode());
    }

    @Test
    void templateMissingRejected() {
        when(planMapper.selectCount(any())).thenReturn(0L);
        DecisionRule rule = ggnRule();
        rule.setResultTemplateId(999L);
        when(ruleMapper.selectList(any())).thenReturn(List.of(rule));
        when(templateMapper.selectById(999L)).thenReturn(null);

        assertThrows(BizException.class, () -> planService.generate(new PlanGenerateRequest(
                1001L, 1, new RuleInputDTO(3, new BigDecimal("4.0"), false, null, null, null), null)));
    }

    @Test
    void lockFailureSurfacesError() throws Exception {
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(false);

        assertThrows(BizException.class, () -> planService.generate(new PlanGenerateRequest(
                1001L, 1, new RuleInputDTO(3, new BigDecimal("4.0"), false, null, null, null), null)));
    }
}
