package com.yiliao.patient.security;

import com.yiliao.common.core.constant.YiliaoConstants;
import com.yiliao.common.core.error.CommonErrorCode;
import com.yiliao.common.core.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * patient 服务角色守卫（specs/modules/patient.md §6）：
 * 患者只能走 /archives/my 与本人档案 detail；员工动作一律拒 PATIENT。
 */
public final class PatientRoleGuard {

    private static final Logger log = LoggerFactory.getLogger(PatientRoleGuard.class);

    private PatientRoleGuard() {
    }

    /** 员工专属：患者调用直接 403。 */
    public static void requireStaff() {
        if (isPatient()) {
            log.warn("患者越权访问 patient 员工接口被拦截");
            throw new BizException(CommonErrorCode.FORBIDDEN);
        }
    }

    /** 档案归属校验：患者只能看自己的档案。 */
    public static void requireOwnArchive(Long archiveUserId) {
        if (!isPatient()) {
            return;
        }
        Long userId = currentUserId();
        if (archiveUserId == null || !archiveUserId.equals(userId)) {
            log.warn("患者越权查看他人档案被拦截 userId={} archiveUserId={}", userId, archiveUserId);
            throw new BizException(CommonErrorCode.FORBIDDEN.withMsg("无权访问他人数据"));
        }
    }

    public static boolean isPatient() {
        String roles = header(YiliaoConstants.HEADER_USER_ROLES);
        return roles != null && List.of(roles.split(",")).contains(YiliaoConstants.ROLE_PATIENT);
    }

    public static Long currentUserId() {
        String userId = header(YiliaoConstants.HEADER_USER_ID);
        if (userId == null || userId.isBlank()) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED);
        }
        return Long.valueOf(userId);
    }

    private static String header(String name) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return request.getHeader(name);
        }
        return null;
    }
}
