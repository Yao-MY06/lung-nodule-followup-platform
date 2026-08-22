package com.yiliao.followup.engine;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskDatesPlannerTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 10);

    @Test
    void firstThenRepeatIntervals() {
        // 首次 6 个月，之后每年（12 个月），总 5 年 → 2026-07-10 起 5 个点
        List<LocalDate> dates = TaskDatesPlanner.plan(START, 6, 12, 5);
        assertEquals(LocalDate.of(2026, 7, 10), dates.get(0));
        assertEquals(LocalDate.of(2027, 7, 10), dates.get(1));
        assertEquals(5, dates.size());
        assertEquals(LocalDate.of(2030, 7, 10), dates.get(4));
    }

    @Test
    void quarterlyPlan() {
        // 首次 3 个月，之后每 3 个月，总 3 年
        List<LocalDate> dates = TaskDatesPlanner.plan(START, 3, 3, 3);
        assertEquals(LocalDate.of(2026, 4, 10), dates.get(0));
        assertEquals(12, dates.size());
        assertTrue(dates.get(dates.size() - 1).isBefore(START.plusYears(3).plusDays(1)));
    }

    @Test
    void deadlineTruncates() {
        // 总年限 1 年、间隔 6 个月：首点 6 个月、次点 12 个月，仅含不超 deadline 的点
        List<LocalDate> dates = TaskDatesPlanner.plan(START, 6, 6, 1);
        assertEquals(2, dates.size());
        assertEquals(LocalDate.of(2027, 1, 10), dates.get(1));
    }

    @Test
    void maxTasksCapped() {
        // 恶意配置：每年 1 个月间隔 × 100 年 → 截断为 300
        List<LocalDate> dates = TaskDatesPlanner.plan(START, 1, 1, 100);
        assertEquals(TaskDatesPlanner.MAX_TASKS, dates.size());
    }

    @Test
    void zeroRepeatIntervalRejected() {
        assertThrows(IllegalArgumentException.class, () -> TaskDatesPlanner.plan(START, 6, 0, 5));
    }
}
