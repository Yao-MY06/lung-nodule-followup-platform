package com.yiliao.common.security.context;

/**
 * 当前用户 ThreadLocal 上下文。
 * P0 仅提供存取；P1 红区任务实现网关头解析过滤器（X-User-Id/X-User-Roles → CurrentUser）与数据权限校验。
 */
public final class SecurityContextHolder {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private SecurityContextHolder() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    /** 无登录上下文返回 null（定时任务/MQ 消费场景）。 */
    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static Long currentUserIdOrNull() {
        CurrentUser user = HOLDER.get();
        return user != null ? user.userId() : null;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
