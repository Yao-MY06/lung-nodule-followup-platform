package com.yiliao.common.core.enums;

/**
 * 公共枚举接口（specs/global/20 §2）。DB 存 code，API 出参传 code，描述由前端字典/VO 提供。
 */
public interface BaseEnum {

    int getCode();

    String getDesc();
}
