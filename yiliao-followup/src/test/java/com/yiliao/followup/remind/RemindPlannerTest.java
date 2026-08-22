package com.yiliao.followup.remind;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemindPlannerTest {

    private static final LocalDate PLAN_DATE = LocalDate.of(2026, 9, 10);

    @Test
    void dueTodayHitsEachOffsetExactlyOnce() {
        assertEquals(1, RemindPlanner.dueToday(PLAN_DATE, LocalDate.of(2026, 9, 3)).size());  // T-7
        assertEquals(1, RemindPlanner.dueToday(PLAN_DATE, LocalDate.of(2026, 9, 7)).size());  // T-3
        assertEquals(1, RemindPlanner.dueToday(PLAN_DATE, LocalDate.of(2026, 9, 9)).size());  // T-1
        assertEquals(0, RemindPlanner.dueToday(PLAN_DATE, LocalDate.of(2026, 9, 10)).size()); // 当天无提醒点
        assertEquals(0, RemindPlanner.dueToday(PLAN_DATE, LocalDate.of(2026, 9, 1)).size());  // 未到
    }

    @Test
    void remindTypeMapping() {
        assertEquals(1, RemindPlanner.remindTypeOf(7));
        assertEquals(2, RemindPlanner.remindTypeOf(3));
        assertEquals(3, RemindPlanner.remindTypeOf(1));
    }

    @Test
    void catchUpFillsOnlyLatestMissingPoint() {
        // 计划日 9-10，今天 9-8：T-7 已过（应发 1 个点），remind_sent=0 → 补 T-7
        List<RemindPlanner.RemindPoint> missed = RemindPlanner.catchUpPoints(PLAN_DATE,
                LocalDate.of(2026, 9, 8), 0);
        assertEquals(1, missed.size());
        assertEquals(1, missed.get(0).remindType());
    }

    @Test
    void catchUpSkipsWhenAlreadySent() {
        // 今天 9-3：仅 T-7 到期，remind_sent=1 已覆盖 → 不补
        List<RemindPlanner.RemindPoint> missed = RemindPlanner.catchUpPoints(PLAN_DATE,
                LocalDate.of(2026, 9, 3), 1);
        assertTrue(missed.isEmpty());
    }

    @Test
    void catchUpFillsNextMissingWhenPartiallySent() {
        // 今天 9-8：T-7 与 T-3 均已到期（应发 2），remind_sent=1 → 还缺 1 个，补最早的缺失点
        List<RemindPlanner.RemindPoint> missed = RemindPlanner.catchUpPoints(PLAN_DATE,
                LocalDate.of(2026, 9, 8), 1);
        assertEquals(1, missed.size());
    }

    @Test
    void catchUpNoPointsBeforeWindow() {
        // 今天 9-2：三个点都未到期 → 无需补
        List<RemindPlanner.RemindPoint> missed = RemindPlanner.catchUpPoints(PLAN_DATE,
                LocalDate.of(2026, 9, 2), 0);
        assertTrue(missed.isEmpty());
    }
}
