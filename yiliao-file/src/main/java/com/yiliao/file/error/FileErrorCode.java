package com.yiliao.file.error;

import com.yiliao.common.core.error.ErrorCode;

/**
 * file 错误码（specs/modules/file.md §5：服务段 26xxx）。
 */
public enum FileErrorCode implements ErrorCode {

    FILE_TYPE_REJECTED(26001, "文件类型不允许"),
    FILE_TOO_LARGE(26002, "文件大小超出限制"),
    OBJECT_KEY_INVALID(26003, "对象键非法");

    private final int code;
    private final String msg;

    FileErrorCode(int code, String msg) {
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
