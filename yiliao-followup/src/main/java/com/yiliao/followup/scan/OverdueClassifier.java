package com.yiliao.followup.scan;

import java.time.LocalDate;

/**
 * 逾期/失访分类器（specs/flows/F2 链路二）。纯函数：
 * 未完成且 plan_date < today-7 → 逾期；逾期且 < today-30 → 失访。
 */
public final class OverdueClassifier {

    public static final int OVERDUE_AFTER_DAYS = 7;
    public static final int LOST_AFTER_DAYS = 30;

    private OverdueClassifier() {
    }

    public enum Outcome { KEEP, MARK_OVERDUE, MARK_LOST }

    public static Outcome classify(Integer status, LocalDate planDate, LocalDate today) {
        boolean open = status != null
                && (status == com.yiliao.followup.entity.FollowupTask.STATUS_PENDING
                || status == com.yiliao.followup.entity.FollowupTask.STATUS_DUE_SOON
                || status == com.yiliao.followup.entity.FollowupTask.STATUS_OVERDUE);
        if (!open || planDate == null) {
            return Outcome.KEEP;
        }
        long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(planDate, today);
        if (overdueDays > LOST_AFTER_DAYS) {
            return Outcome.MARK_LOST;
        }
        if (overdueDays > OVERDUE_AFTER_DAYS) {
            return Outcome.MARK_OVERDUE;
        }
        return Outcome.KEEP;
    }
}
