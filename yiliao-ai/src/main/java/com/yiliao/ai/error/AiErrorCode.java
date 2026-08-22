package com.yiliao.ai.error;

import com.yiliao.common.core.error.ErrorCode;

/**
 * ai 错误码（specs/global/10 §4 服务段 24xxx）。
 */
public enum AiErrorCode implements ErrorCode {

    LLM_DEGRADED(24001, "AI 服务暂时不可用，请稍后再试或联系您的医生"),
    EXTRACT_FAILED(24002, "报告抽取失败，请手动录入");

    private final int code;
    private final String msg;

    AiErrorCode(int code, String msg) {
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
