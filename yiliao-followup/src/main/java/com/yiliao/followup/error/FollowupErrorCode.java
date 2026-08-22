package com.yiliao.followup.error;

import com.yiliao.common.core.error.ErrorCode;

/**
 * followup 错误码（specs/global/10 §4 服务段 22xxx）。
 */
public enum FollowupErrorCode implements ErrorCode {

    PLAN_ALREADY_EXISTS(22001, "该患者已有进行中计划"),
    RULE_NOT_FOUND(22002, "无可用随访规则，请维护 decision_rule"),
    PLAN_NOT_FOUND(22003, "随访计划不存在"),
    TASK_STATUS_ILLEGAL(22004, "任务状态不允许该操作"),
    TASK_NOT_FOUND(22005, "随访任务不存在"),
    RULE_UPDATE_ILLEGAL(22006, "规则字段非法");

    private final int code;
    private final String msg;

    FollowupErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    @Override
    public ErrorCode withMsg(String message) {
        return new Custom(code, message);
    }

    record Custom(int code, String msg) implements ErrorCode {
        @Override
        public int getCode() {
            return code;
        }

        @Override
        public String getMsg() {
            return msg;
        }

        @Override
        public ErrorCode withMsg(String message) {
            return new Custom(code, message);
        }
    }
}
