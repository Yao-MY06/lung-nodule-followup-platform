package com.yiliao.patient.error;

import com.yiliao.common.core.error.ErrorCode;

/**
 * patient 错误码（specs/global/10 §4 服务段 20xxx）。
 */
public enum PatientErrorCode implements ErrorCode {

    DUPLICATE_ARCHIVE(20001, "档案已存在或正在创建，请勿重复提交"),
    STAGE_TRANSITION_ILLEGAL(20002, "该阶段流转不被允许"),
    ARCHIVE_NOT_FOUND(20003, "档案不存在");

    private final int code;
    private final String msg;

    PatientErrorCode(int code, String msg) {
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
