package com.yiliao.auth.error;

import com.yiliao.common.core.error.ErrorCode;

/**
 * auth 错误码（specs/global/10 §4 服务段 10xxx）。
 */
public enum AuthErrorCode implements ErrorCode {

    BAD_CREDENTIALS(10001, "用户名或密码错误"),
    ACCOUNT_LOCKED(10002, "登录失败次数过多，账号已临时锁定"),
    ACCOUNT_DISABLED(10003, "账号已停用"),
    TOKEN_INVALID(10004, "登录凭证无效或已过期"),
    REFRESH_INVALID(10005, "刷新凭证无效或已过期");

    private final int code;
    private final String msg;

    AuthErrorCode(int code, String msg) {
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
        return new Common(this.code, msg);
    }

    record Common(int code, String msg) implements ErrorCode {
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
            return new Common(code, message);
        }
    }
}
