package com.yiliao.common.core.error;

/**
 * 错误码接口（specs/global/10 §4）。
 * 业务错误码 5 位 = 服务段 2 位 + 序号 3 位，段值见全局规范；通用码：0/401/403/404/500。
 */
public interface ErrorCode {

    int getCode();

    String getMsg();

    ErrorCode withMsg(String msg);
}
