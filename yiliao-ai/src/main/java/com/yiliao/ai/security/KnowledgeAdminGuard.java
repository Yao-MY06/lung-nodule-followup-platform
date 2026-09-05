package com.yiliao.ai.security;

import com.yiliao.common.core.constant.YiliaoConstants;
import com.yiliao.common.core.error.CommonErrorCode;
import com.yiliao.common.core.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * 知识库写入权限守卫：仅允许网关透传 ADMIN 角色的请求。
 * 无请求上下文或缺少角色时默认拒绝，避免后台任务上下文意外获得写权限。
 */
public final class KnowledgeAdminGuard {

    private KnowledgeAdminGuard() {
    }

    public static void requireAdmin() {
        String roles = header(YiliaoConstants.HEADER_USER_ROLES);
        boolean admin = roles != null && Arrays.stream(roles.split(","))
                .map(String::trim)
                .anyMatch(YiliaoConstants.ROLE_ADMIN::equals);
        if (!admin) {
            throw new BizException(CommonErrorCode.FORBIDDEN);
        }
    }

    private static String header(String name) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return request.getHeader(name);
        }
        return null;
    }
}
