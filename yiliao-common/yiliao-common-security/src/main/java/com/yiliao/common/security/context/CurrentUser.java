package com.yiliao.common.security.context;

import java.util.List;

/**
 * 当前登录用户模型（specs/global/10 §5）。来源：网关透传 X-User-Id/X-User-Roles 解析结果。
 */
public record CurrentUser(Long userId, String username, List<String> roles) {

    public boolean hasRole(String roleCode) {
        return roles != null && roles.contains(roleCode);
    }

    public boolean isPatient() {
        return hasRole(com.yiliao.common.core.constant.YiliaoConstants.ROLE_PATIENT);
    }
}
