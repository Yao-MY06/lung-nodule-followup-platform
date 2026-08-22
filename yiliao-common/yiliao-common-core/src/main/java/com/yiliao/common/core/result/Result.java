package com.yiliao.common.core.result;

import com.yiliao.common.core.error.ErrorCode;

/**
 * 统一返回体（specs/global/10 §2）。成功 code=0；禁止 Controller 绕过本类返回裸对象。
 */
public record Result<T>(int code, String msg, T data) {

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMsg(), null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    public boolean isSuccess() {
        return code == 0;
    }
}
