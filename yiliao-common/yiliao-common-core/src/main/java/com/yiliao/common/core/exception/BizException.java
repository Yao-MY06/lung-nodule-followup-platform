package com.yiliao.common.core.exception;

import com.yiliao.common.core.error.ErrorCode;

/**
 * 业务异常（specs/global/10 §7）。service 层抛出，由 common-web 全局异常处理器统一转 Result。
 * 禁止 catch 后既不打日志也不抛出。
 */
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String msg) {
        super(msg);
        this.errorCode = errorCode.withMsg(msg);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getCode() {
        return errorCode.getCode();
    }
}
