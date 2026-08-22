package com.yiliao.followup.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiliao.api.followup.dto.RuleInputDTO;
import com.yiliao.followup.entity.DecisionRule;
import com.yiliao.followup.error.FollowupErrorCode;
import com.yiliao.followup.mapper.DecisionRuleMapper;
import com.yiliao.common.core.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 表驱动规则引擎（specs/modules/followup.md §4.1）：加载启用规则 → 条件求值 → priority 最高者胜出。
 * 规则只在 decision_rule 表维护，禁止在 Java 写随访间隔（模块禁止行为 #1）。
 * 缓存（Caffeine+Redis 两级）P3 接入，当前直查（正确性优先）。
 */
@Service
public class RuleEngineService {

    private static final Logger log = LoggerFactory.getLogger(RuleEngineService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DecisionRuleMapper decisionRuleMapper;

    public RuleEngineService(DecisionRuleMapper decisionRuleMapper) {
        this.decisionRuleMapper = decisionRuleMapper;
    }

    /** 命中多条取 priority 最高（同优先级取 id 小者，保证确定性）；无命中抛 22002，禁止默认间隔兜底。 */
    public DecisionRule match(Integer scene, RuleInputDTO input) {
        List<DecisionRule> rules = decisionRuleMapper.selectList(new LambdaQueryWrapper<DecisionRule>()
                .eq(DecisionRule::getScene, scene)
                .eq(DecisionRule::getStatus, 1));

        return rules.stream()
                .filter(rule -> evaluate(rule, input))
                .max(Comparator.comparingInt(DecisionRule::getPriority)
                        .thenComparing(Comparator.comparingLong(DecisionRule::getId).reversed()))
                .orElseThrow(() -> {
                    log.warn("规则无命中 scene={} input={}", scene, input);
                    return new BizException(FollowupErrorCode.RULE_NOT_FOUND);
                });
    }

    private boolean evaluate(DecisionRule rule, RuleInputDTO input) {
        try {
            Map<String, Object> conditions = MAPPER.readValue(rule.getCondJson(),
                    new TypeReference<Map<String, Object>>() {
                    });
            return ConditionEvaluator.matches(conditions, input);
        } catch (Exception e) {
            // 单条规则 cond_json 脏数据不影响其他规则
            log.error("规则 cond_json 解析失败 ruleId={} cond={}", rule.getId(), rule.getCondJson(), e);
            return false;
        }
    }
}
