package com.yiliao.statistics.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 指标口径单测（specs/modules/statistics.md §4.2）：正常值、舍入、除零、边界防御。
 */
class StatsCalculatorTest {

    @Test
    void completionRateNormal() {
        assertEquals(0.91, StatsCalculator.completionRate(100, 91), 1e-9);
    }

    @Test
    void completionRateRoundsToFourDigitsHalfUp() {
        // 2/3 = 0.66666... → 0.6667
        assertEquals(0.6667, StatsCalculator.completionRate(3, 2), 1e-9);
    }

    @Test
    void completionRateFull() {
        assertEquals(1.0, StatsCalculator.completionRate(50, 50), 1e-9);
    }

    @Test
    void completionRateZeroDueReturnsZero() {
        assertEquals(0.0, StatsCalculator.completionRate(0, 0), 0.0);
    }

    @Test
    void completionRateZeroDueWithDoneReturnsZero() {
        // 边界防御：due=0 而 done>0 业务上不应出现，返回 0.0 而非除零异常
        assertEquals(0.0, StatsCalculator.completionRate(0, 5), 0.0);
    }

    @Test
    void timelyRateNormal() {
        // 401/480 = 0.8354166... → 0.8354
        assertEquals(0.8354, StatsCalculator.timelyRate(480, 401), 1e-9);
    }

    @Test
    void timelyRateZeroDueReturnsZero() {
        assertEquals(0.0, StatsCalculator.timelyRate(0, 3), 0.0);
    }

    @Test
    void timelyRateFull() {
        assertEquals(1.0, StatsCalculator.timelyRate(12, 12), 1e-9);
    }
}
