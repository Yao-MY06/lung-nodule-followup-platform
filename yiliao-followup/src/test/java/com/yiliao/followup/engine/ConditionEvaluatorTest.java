package com.yiliao.followup.engine;

import com.yiliao.api.followup.dto.RuleInputDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionEvaluatorTest {

    private RuleInputDTO input(Integer noduleType, String maxDia, Boolean risk) {
        return new RuleInputDTO(noduleType, maxDia == null ? null : new BigDecimal(maxDia), risk, null, null, null);
    }

    @Test
    void numericOperators() {
        assertTrue(ConditionEvaluator.matches(Map.of("max_dia", "<=5"), input(3, "5.0", null)));
        assertTrue(ConditionEvaluator.matches(Map.of("max_dia", "<=5"), input(3, "4.9", null)));
        assertFalse(ConditionEvaluator.matches(Map.of("max_dia", "<=5"), input(3, "5.1", null)));
        assertTrue(ConditionEvaluator.matches(Map.of("max_dia", ">5"), input(3, "5.1", null)));
        assertFalse(ConditionEvaluator.matches(Map.of("max_dia", ">5"), input(3, "5.0", null)));
    }

    @Test
    void closedRange() {
        assertTrue(ConditionEvaluator.matches(Map.of("max_dia", "4-6"), input(1, "4.0", null)));
        assertTrue(ConditionEvaluator.matches(Map.of("max_dia", "4-6"), input(1, "6.0", null)));
        assertFalse(ConditionEvaluator.matches(Map.of("max_dia", "4-6"), input(1, "6.1", null)));
        assertFalse(ConditionEvaluator.matches(Map.of("max_dia", "4-6"), input(1, "3.9", null)));
    }

    @Test
    void boundaryOverlapHitsHigherPriorityRule() {
        // 6-8 与 <=4/4-6 的边界：6.0mm 应命中 6-8
        assertTrue(ConditionEvaluator.matches(Map.of("max_dia", "6-8"), input(1, "6.0", null)));
        assertFalse(ConditionEvaluator.matches(Map.of("max_dia", "4-6"), input(1, "6.1", null)));
    }

    @Test
    void enumAndBoolean() {
        assertTrue(ConditionEvaluator.matches(Map.of("nodule_type", 3), input(3, "4.0", null)));
        assertFalse(ConditionEvaluator.matches(Map.of("nodule_type", 3), input(1, "4.0", null)));
        assertTrue(ConditionEvaluator.matches(Map.of("risk", false), input(1, "5.0", false)));
        assertTrue(ConditionEvaluator.matches(Map.of("risk", false), input(1, "5.0", null)));
        assertFalse(ConditionEvaluator.matches(Map.of("risk", false), input(1, "5.0", true)));
    }

    @Test
    void combinedConditionsAllMustHold() {
        Map<String, Object> cond = Map.of("nodule_type", 1, "max_dia", "6-8", "risk", true);
        assertTrue(ConditionEvaluator.matches(cond, input(1, "7.0", true)));
        assertFalse(ConditionEvaluator.matches(cond, input(1, "7.0", false)));
        assertFalse(ConditionEvaluator.matches(cond, input(3, "7.0", true)));
    }

    @Test
    void missingFeatureNeverMatches() {
        assertFalse(ConditionEvaluator.matches(Map.of("max_dia", "<=5"), input(3, null, null)));
    }

    @Test
    void emptyConditionMatchesAsFallback() {
        assertTrue(ConditionEvaluator.matches(Map.of(), input(null, null, null)));
    }

    @Test
    void unknownConditionKeyFailsClosed() {
        assertFalse(ConditionEvaluator.matches(Map.of("unknown_key", "x"), input(3, "4.0", null)));
    }

    @Test
    void stageGroupCaseInsensitive() {
        var postOp = new RuleInputDTO(null, null, null, "II_III", null, null);
        assertTrue(ConditionEvaluator.matches(Map.of("stage_group", "II_III"), postOp));
        assertTrue(ConditionEvaluator.matches(Map.of("stage_group", "ii_iii"), postOp));
        assertFalse(ConditionEvaluator.matches(Map.of("stage_group", "I"), postOp));
    }
}
