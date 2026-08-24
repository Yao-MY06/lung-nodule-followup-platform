package com.yiliao.nodule.security;

import com.yiliao.api.patient.PatientApi;
import com.yiliao.common.core.constant.YiliaoConstants;
import com.yiliao.common.core.error.CommonErrorCode;
import com.yiliao.common.core.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 患者数据归属守卫（specs/modules/nodule.md 禁止行为；AGENTS.md 不变量）。
 * 规则：含 PATIENT 角色 → patientId 必须等于本人档案；员工角色放行；
 * 无请求上下文（XXL-Job/MQ 消费）视为系统调用放行。
 * 内部 Feign 契约（internal 路径前缀）不走本守卫——由服务自身或工具层保证（AI 工具已强制当前患者）。
 */
@Component
public class PatientDataGuard {

    private static final Logger log = LoggerFactory.getLogger(PatientDataGuard.class);

    private final PatientApi patientApi;

    public PatientDataGuard(PatientApi patientApi) {
        this.patientApi = patientApi;
    }

    /** 员工专属接口：患者调用直接 403。 */
    public void requireStaff() {
        if (isPatient()) {
            log.warn("患者越权访问员工接口被拦截");
            throw new BizException(CommonErrorCode.FORBIDDEN);
        }
    }

    /** 患者侧接口：患者只能访问本人档案对应的数据。 */
    public void requireOwnOrStaff(Long patientId) {
        if (!isPatient()) {
            return; // 员工或系统上下文
        }
        Long userId = currentUserId();
        Long mine = resolveMyArchiveId(userId);
        if (mine == null || !mine.equals(patientId)) {
            log.warn("患者越权访问他人数据被拦截 userId={} requestedPatientId={}", userId, patientId);
            throw new BizException(CommonErrorCode.FORBIDDEN.withMsg("无权访问他人数据"));
        }
    }

    private boolean isPatient() {
        List<String> roles = currentRoles();
        return roles != null && roles.contains(YiliaoConstants.ROLE_PATIENT);
    }

    private Long resolveMyArchiveId(Long userId) {
        try {
            var archive = patientApi.getArchiveByUserId(userId).data();
            return archive == null ? null : archive.id();
        } catch (Exception e) {
            log.error("档案解析失败，拒绝访问 userId={}", userId, e);
            return null; // 解析失败=拒绝（宁可拒绝不可放行）
        }
    }

    private List<String> currentRoles() {
        String roles = header(YiliaoConstants.HEADER_USER_ROLES);
        if (roles == null || roles.isBlank()) {
            return null;
        }
        return List.of(roles.split(","));
    }

    private Long currentUserId() {
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
        return null; // 系统上下文（Job/MQ）
    }
}
