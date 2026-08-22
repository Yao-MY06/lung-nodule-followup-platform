package com.yiliao.common.data.fill;

/**
 * 操作人来源抽象：data 模块不依赖 security（common 间只依赖 core，specs/global/30 §3），
 * 由服务侧提供实现（从网关透传头或 MQ 上下文取当前用户）。
 */
public interface UserContextProvider {

    /** 当前操作者用户 id；系统上下文（无登录态）返回 null。 */
    Long currentUserId();
}
