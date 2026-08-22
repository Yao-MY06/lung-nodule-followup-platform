package com.yiliao.common.mq.constant;

/**
 * MQ 主题契约（specs/global/30 §6）。命名 YILIAO_{领域}_{事件}；新增主题必须同步该文件与 G30 文档。
 */
public final class Topics {

    private Topics() {
    }

    /** patient → followup：建档完成兜底对账 */
    public static final String ARCHIVE_CREATED = "YILIAO_ARCHIVE_CREATED";

    /** nodule → followup：AI 抽取确认入库 → 生成/更新计划 */
    public static final String EXTRACT_CONFIRMED = "YILIAO_EXTRACT_CONFIRMED";

    /** followup → notification：计划生成 → 排程提醒 */
    public static final String PLAN_CREATED = "YILIAO_PLAN_CREATED";

    /** followup → notification（定时消息）：到点提醒，bizKey=taskId:remindType */
    public static final String REMIND_DUE = "YILIAO_REMIND_DUE";

    /** nodule → followup、notification：结节增大/实性成分 +≥2mm */
    public static final String NODULE_PROGRESS = "YILIAO_NODULE_PROGRESS";

    /** followup → notification：ePRO severity≥3 推送医生 */
    public static final String SYMPTOM_ALERT = "YILIAO_SYMPTOM_ALERT";
}
