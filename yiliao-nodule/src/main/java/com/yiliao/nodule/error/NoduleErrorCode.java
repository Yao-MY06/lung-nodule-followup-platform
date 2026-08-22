package com.yiliao.nodule.error;

import com.yiliao.common.core.error.ErrorCode;

/**
 * nodule 错误码（specs/global/10 §4 服务段 21xxx）。
 */
public enum NoduleErrorCode implements ErrorCode {

    SNAPSHOT_INVALID(21001, "快照缺少检查日期或结节"),
    REPORT_NOT_FOUND(21002, "检查报告不存在"),
    STRUCTURED_JSON_INVALID(21003, "抽取结果缺失或不合法"),
    ALREADY_CONFIRMED(21004, "该报告已确认入库，禁止重复确认");

    private final int code;
    private final String msg;

    NoduleErrorCode(int code, String msg) {
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
