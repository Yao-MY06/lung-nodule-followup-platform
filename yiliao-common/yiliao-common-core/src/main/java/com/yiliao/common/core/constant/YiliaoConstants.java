package com.yiliao.common.core.constant;

/**
 * 全局常量（specs/global/20 §6）。缓存 key 前缀 yiliao:{service}:{biz}:{id}；分布式锁 yiliao:lock:{biz}:{id}。
 */
public final class YiliaoConstants {

    private YiliaoConstants() {
    }

    /** 网关校验通过后透传的内部用户头（specs/global/10 §5） */
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLES = "X-User-Roles";
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    /** 缓存/锁 key 前缀 */
    public static final String CACHE_PREFIX = "yiliao:";
    public static final String LOCK_PREFIX = "yiliao:lock:";
    public static final String MQ_CONSUMED_PREFIX = "yiliao:mq:consumed:";
    /** Token 黑名单前缀（auth 写入，gateway 校验，格式必须一致） */
    public static final String TOKEN_BLACKLIST_PREFIX = "yiliao:auth:blacklist:";

    /** MQ 消费幂等 key TTL（天） */
    public static final int MQ_CONSUMED_TTL_DAYS = 7;

    /** 角色 code（与 sys_role.role_code 一致） */
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_DOCTOR = "DOCTOR";
    public static final String ROLE_NURSE = "NURSE";
    public static final String ROLE_PATIENT = "PATIENT";
}
