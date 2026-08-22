package com.yiliao.common.core.error;

/**
 * 通用错误码（specs/global/10 §4）。业务错误码在各模块 ErrorCode 常量类中定义。
 */
public enum CommonErrorCode implements ErrorCode {

    SUCCESS(0, "成功"),
    UNAUTHORIZED(401, "未认证或登录已过期"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "资源不存在"),
    PARAM_INVALID(400, "请求参数错误"),
    SYSTEM_ERROR(500, "系统繁忙，请稍后重试");

    private final int code;
    private final String msg;

    CommonErrorCode(int code, String msg) {
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
    public ErrorCode withMsg(String msg) {
        return new SimpleErrorCode(this.code, msg);
    }

    /** 携带自定义消息的错误码实例（code 仍取枚举值）。 */
    public record SimpleErrorCode(int code, String msg) implements ErrorCode {
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
            return new SimpleErrorCode(code, message);
        }
    }
}
