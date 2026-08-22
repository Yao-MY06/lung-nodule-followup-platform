package com.yiliao.followup.engine;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务排期器（specs/modules/followup.md §4.1）。纯函数：
 * 首次 = start + firstInterval；之后按 repeatInterval 递进，直至超过 start + totalYears；上限 300 条防脏数据撑爆表。
 */
public final class TaskDatesPlanner {

    public static final int MAX_TASKS = 300;

    private TaskDatesPlanner() {
    }

    public static List<LocalDate> plan(LocalDate start, int firstIntervalMonth, int repeatIntervalMonth,
                                       int totalYears) {
        if (repeatIntervalMonth <= 0) {
            throw new IllegalArgumentException("repeatIntervalMonth 必须 > 0");
        }
        LocalDate deadline = start.plusYears(totalYears);
        List<LocalDate> dates = new ArrayList<>();
        LocalDate first = start.plusMonths(firstIntervalMonth);
        if (first.isAfter(deadline)) {
            return dates;
        }
        dates.add(first);
        LocalDate cursor = first;
        while (dates.size() < MAX_TASKS) {
            LocalDate next = cursor.plusMonths(repeatIntervalMonth);
            if (next.isAfter(deadline)) {
                break;
            }
            dates.add(next);
            cursor = next;
        }
        return dates;
    }
}
