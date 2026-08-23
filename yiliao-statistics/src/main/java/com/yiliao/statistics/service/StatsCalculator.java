package com.yiliao.statistics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 指标口径纯函数（specs/modules/statistics.md §4.2，口径固化于此、无 Spring 依赖）：
 * 完成率 = 已完成任务 / 应完成任务（按计划日期 &lt;= 今日）；
 * 及时率 = done_date &lt;= plan_date 的任务占比；
 * 分母为 0 一律返回 0.0（due=0 而 done&gt;0 业务上不应出现，此处防御性返回 0），结果保留 4 位小数。
 */
public final class StatsCalculator {

    private static final int SCALE = 4;

    private StatsCalculator() {
    }

    /** 完成率 = done / due，HALF_UP 保留 4 位小数。 */
    public static double completionRate(int due, int done) {
        return rate(due, done);
    }

    /** 及时率 = timely / due，HALF_UP 保留 4 位小数。 */
    public static double timelyRate(int due, int timely) {
        return rate(due, timely);
    }

    private static double rate(int denominator, int numerator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
