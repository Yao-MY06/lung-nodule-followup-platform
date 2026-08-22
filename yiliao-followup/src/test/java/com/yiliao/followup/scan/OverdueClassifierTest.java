package com.yiliao.followup.scan;

import com.yiliao.followup.entity.FollowupTask;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverdueClassifierTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 22);

    @Test
    void sevenDaysOverdueMarked() {
        // 计划日 8-14，今天 8-22：逾期 8 天 > 7 → 逾期（F2 验证清单 #3）
        assertEquals(OverdueClassifier.Outcome.MARK_OVERDUE,
                OverdueClassifier.classify(FollowupTask.STATUS_PENDING, LocalDate.of(2026, 8, 14), TODAY));
    }

    @Test
    void exactlySevenDaysKept() {
        // 恰好 7 天：未超阈值，不误伤
        assertEquals(OverdueClassifier.Outcome.KEEP,
                OverdueClassifier.classify(FollowupTask.STATUS_PENDING, LocalDate.of(2026, 8, 15), TODAY));
    }

    @Test
    void overThirtyDaysLost() {
        assertEquals(OverdueClassifier.Outcome.MARK_LOST,
                OverdueClassifier.classify(FollowupTask.STATUS_OVERDUE, LocalDate.of(2026, 7, 20), TODAY));
    }

    @Test
    void completedOrCancelledNeverTouched() {
        assertEquals(OverdueClassifier.Outcome.KEEP,
                OverdueClassifier.classify(FollowupTask.STATUS_DONE, LocalDate.of(2026, 6, 1), TODAY));
        assertEquals(OverdueClassifier.Outcome.KEEP,
                OverdueClassifier.classify(FollowupTask.STATUS_CANCELLED, LocalDate.of(2026, 6, 1), TODAY));
        assertEquals(OverdueClassifier.Outcome.KEEP,
                OverdueClassifier.classify(null, LocalDate.of(2026, 6, 1), TODAY));
    }

    @Test
    void futurePlanDateKept() {
        assertEquals(OverdueClassifier.Outcome.KEEP,
                OverdueClassifier.classify(FollowupTask.STATUS_PENDING, LocalDate.of(2026, 9, 1), TODAY));
    }
}
