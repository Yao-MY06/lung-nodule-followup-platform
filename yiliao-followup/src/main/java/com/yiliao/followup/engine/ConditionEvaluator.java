package com.yiliao.followup.engine;

import com.yiliao.api.followup.dto.RuleInputDTO;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 条件求值器（specs/modules/followup.md §4.1）。纯函数，无状态。
 * cond_json 语法：{"nodule_type":1,"max_dia":"<=5","risk":false,"stage_group":"II_III",...}
 * 数值运算符："<=x" ">=x" "<x" ">x" "a-b"(闭区间) 纯数字(相等)；布尔缺省按 false 处理。
 */
public final class ConditionEvaluator {

    private static final Pattern OP_PATTERN = Pattern.compile("^(<=|>=|<|>)\\s*([0-9.]+)$");
    private static final Pattern RANGE_PATTERN = Pattern.compile("^([0-9.]+)\\s*-\\s*([0-9.]+)$");

    private ConditionEvaluator() {
    }

    /** 全部条件项满足才命中；空条件视为命中（作为兜底规则）。 */
    public static boolean matches(Map<String, Object> conditions, RuleInputDTO input) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            if (!evalOne(entry.getKey(), entry.getValue(), input)) {
                return false;
            }
        }
        return true;
    }

    private static boolean evalOne(String key, Object condValue, RuleInputDTO input) {
        return switch (key) {
            case "nodule_type" -> intEquals(condValue, input.noduleType());
            case "max_dia" -> numeric(input.maxDiaMm(), str(condValue));
            case "risk" -> boolEquals(condValue, boolFeature(input.riskFactor()));
            case "gene_positive" -> boolEquals(condValue, boolFeature(input.genePositive()));
            case "adjuvant" -> boolEquals(condValue, boolFeature(input.adjuvantTherapy()));
            case "stage_group" -> str(condValue) == null
                    || str(condValue).equalsIgnoreCase(nullSafe(input.stageGroup()));
            // 未知条件键：保守不命中，避免规则语义静默漂移
            default -> false;
        };
    }

    private static boolean numeric(BigDecimal feature, String expression) {
        if (feature == null || expression == null) {
            return false;
        }
        Matcher op = OP_PATTERN.matcher(expression.trim());
        if (op.matches()) {
            BigDecimal bound = new BigDecimal(op.group(2));
            return switch (op.group(1)) {
                case "<=" -> feature.compareTo(bound) <= 0;
                case ">=" -> feature.compareTo(bound) >= 0;
                case "<" -> feature.compareTo(bound) < 0;
                default -> feature.compareTo(bound) > 0;
            };
        }
        Matcher range = RANGE_PATTERN.matcher(expression.trim());
        if (range.matches()) {
            BigDecimal low = new BigDecimal(range.group(1));
            BigDecimal high = new BigDecimal(range.group(2));
            return feature.compareTo(low) >= 0 && feature.compareTo(high) <= 0;
        }
        try {
            return feature.compareTo(new BigDecimal(expression.trim())) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean intEquals(Object condValue, Integer feature) {
        if (condValue instanceof Number number) {
            return feature != null && number.intValue() == feature;
        }
        try {
            return feature != null && Integer.parseInt(str(condValue)) == feature;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean boolEquals(Object condValue, boolean feature) {
        if (condValue instanceof Boolean bool) {
            return bool == feature;
        }
        return Boolean.parseBoolean(str(condValue)) == feature;
    }

    private static boolean boolFeature(Boolean feature) {
        return Boolean.TRUE.equals(feature);
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
