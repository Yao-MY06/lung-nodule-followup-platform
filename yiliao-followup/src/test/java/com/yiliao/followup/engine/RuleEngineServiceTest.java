package com.yiliao.followup.engine;

import com.yiliao.api.followup.dto.RuleInputDTO;
import com.yiliao.followup.entity.DecisionRule;
import com.yiliao.followup.error.FollowupErrorCode;
import com.yiliao.common.core.exception.BizException;
import com.yiliao.followup.mapper.DecisionRuleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleEngineServiceTest {

    @Mock
    private DecisionRuleMapper ruleMapper;

    private DecisionRule rule(long id, String condJson, int priority) {
        DecisionRule rule = new DecisionRule();
        rule.setId(id);
        rule.setScene(1);
        rule.setCondJson(condJson);
        rule.setPriority(priority);
        rule.setStatus(1);
        return rule;
    }

    @Test
    void highestPriorityWinsOnMultipleHits() {
        // 6.0mm 磨玻璃同时命中 <=5 与 >5 边界外的规则集：>5 的规则应胜出
        when(ruleMapper.selectList(any())).thenReturn(List.of(
                rule(7, "{\"nodule_type\":3,\"max_dia\":\"<=5\"}", 40),
                rule(8, "{\"nodule_type\":3,\"max_dia\":\">5\"}", 40)));
        RuleEngineService service = new RuleEngineService(ruleMapper);

        DecisionRule matched = service.match(1,
                new RuleInputDTO(3, new BigDecimal("6.0"), false, null, null, null));
        assertEquals(8L, matched.getId());
    }

    @Test
    void riskFactorSelectsMoreIntensiveRule() {
        when(ruleMapper.selectList(any())).thenReturn(List.of(
                rule(2, "{\"nodule_type\":1,\"max_dia\":\"4-6\",\"risk\":false}", 20),
                rule(5, "{\"nodule_type\":1,\"max_dia\":\"4-6\",\"risk\":true}", 30)));
        RuleEngineService service = new RuleEngineService(ruleMapper);

        DecisionRule noRisk = service.match(1,
                new RuleInputDTO(1, new BigDecimal("5.0"), false, null, null, null));
        DecisionRule withRisk = service.match(1,
                new RuleInputDTO(1, new BigDecimal("5.0"), true, null, null, null));
        assertEquals(2L, noRisk.getId());
        assertEquals(5L, withRisk.getId());
    }

    @Test
    void noMatchThrowsInsteadOfDefaultInterval() {
        // 禁止默认间隔兜底（模块禁止行为 #1 的测试化表达）
        when(ruleMapper.selectList(any())).thenReturn(List.of(
                rule(1, "{\"nodule_type\":1,\"max_dia\":\"<=4\"}", 20)));
        RuleEngineService service = new RuleEngineService(ruleMapper);

        BizException e = assertThrows(BizException.class, () -> service.match(1,
                new RuleInputDTO(1, new BigDecimal("20.0"), false, null, null, null)));
        assertEquals(FollowupErrorCode.RULE_NOT_FOUND.getCode(), e.getCode());
    }

    @Test
    void malformedRuleSkippedNotFatal() {
        when(ruleMapper.selectList(any())).thenReturn(List.of(
                rule(1, "{broken-json", 99),
                rule(2, "{\"nodule_type\":1}", 10)));
        RuleEngineService service = new RuleEngineService(ruleMapper);

        DecisionRule matched = service.match(1,
                new RuleInputDTO(1, new BigDecimal("5.0"), false, null, null, null));
        assertEquals(2L, matched.getId());
    }
}
