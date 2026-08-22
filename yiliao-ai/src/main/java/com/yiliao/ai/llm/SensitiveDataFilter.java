package com.yiliao.ai.llm;

import java.util.List;
import java.util.regex.Pattern;

/**
 * AI 出网前脱敏过滤器（specs/modules/ai.md §4.1，不变量）：
 * 手机号/身份证/邮箱/住院号替换为占位词；姓名无法可靠识别，靠 Prompt 约束补位。
 */
public final class SensitiveDataFilter {

    private record Rule(Pattern pattern, String replacement) {
    }

    // 顺序即优先级：长模式（证件号）必须先于短模式（手机号），否则证件号内的 11 位子串会被误判为手机号
    private static final List<Rule> RULES = List.of(
            new Rule(Pattern.compile("\\d{17}[0-9Xx]"), "[证件号]"),
            new Rule(Pattern.compile("\\d{15}"), "[证件号]"),
            new Rule(Pattern.compile("1[3-9]\\d{9}"), "[电话]"),
            new Rule(Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.]+"), "[邮箱]"),
            new Rule(Pattern.compile("(住院号|门诊号|病历号)[:：\\s]*\\d+"), "$1[已隐藏]")
    );

    private SensitiveDataFilter() {
    }

    public static String sanitize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = text;
        for (Rule rule : RULES) {
            result = rule.pattern().matcher(result).replaceAll(rule.replacement());
        }
        return result;
    }

    /** 是否仍含明显敏感残留（证件号/手机号，用于自检与测试）。 */
    public static boolean containsSensitive(String text) {
        if (text == null) {
            return false;
        }
        return RULES.stream().limit(3).anyMatch(rule -> rule.pattern().matcher(text).find());
    }
}
