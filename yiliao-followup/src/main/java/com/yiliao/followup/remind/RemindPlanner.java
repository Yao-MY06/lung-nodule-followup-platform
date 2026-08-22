package com.yiliao.followup.remind;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 提醒点计算（specs/flows/F2 链路一）。纯函数。
 * T-7/T-3/T-1：提醒点已过的不派发（首次上线/补数场景），仅对"今天应发"的点派发；
 * 对账模式（catchUp=true）用于 XXL-Job 每小时补投：把已过但 remind_sent 未覆盖的点补上。
 */
public final class RemindPlanner {

    public static final int[] OFFSET_DAYS = {7, 3, 1};

    private RemindPlanner() {
    }

    /** 提醒类型码（specs/global/20 RemindType）：1 T-7，2 T-3，3 T-1。 */
    public static int remindTypeOf(int offsetDays) {
        return switch (offsetDays) {
            case 7 -> 1;
            case 3 -> 2;
            case 1 -> 3;
            default -> throw new IllegalArgumentException("不支持的提醒偏移: " + offsetDays);
        };
    }

    public record RemindPoint(int remindType, int offsetDays, LocalDate remindDate) {
    }

    /** 今天应派的提醒点（remindDate == today），任务完成/取消的由调用方过滤。 */
    public static List<RemindPoint> dueToday(LocalDate planDate, LocalDate today) {
        List<RemindPoint> points = new ArrayList<>();
        for (int offset : OFFSET_DAYS) {
            LocalDate remindDate = planDate.minusDays(offset);
            if (remindDate.isEqual(today)) {
                points.add(new RemindPoint(remindTypeOf(offset), offset, remindDate));
            }
        }
        return points;
    }

    /**
     * 对账：已到期（remindDate <= today）但发送计数未覆盖的点。
     * remindSent 语义：已发送的提醒次数（每次成功 +1）；由于点数最多 3，用"应发点数 > remindSent"判断缺发。
     */
    public static List<RemindPoint> catchUpPoints(LocalDate planDate, LocalDate today, int remindSent) {
        List<RemindPoint> missed = new ArrayList<>();
        int dueCount = 0;
        for (int offset : OFFSET_DAYS) {
            if (!planDate.minusDays(offset).isAfter(today)) {
                dueCount++;
            }
        }
        // 简化对账：只补最近一个缺失点（避免一次性轰炸），其余靠每日增量
        if (dueCount > remindSent) {
            for (int offset : OFFSET_DAYS) {
                LocalDate remindDate = planDate.minusDays(offset);
                if (!remindDate.isAfter(today)) {
                    missed.add(new RemindPoint(remindTypeOf(offset), offset, remindDate));
                    break;
                }
            }
        }
        return missed;
    }
}
