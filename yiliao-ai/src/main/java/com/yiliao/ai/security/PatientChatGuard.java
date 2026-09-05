package com.yiliao.ai.security;

import com.yiliao.api.patient.PatientApi;
import com.yiliao.api.patient.dto.ArchiveDTO;
import com.yiliao.common.core.constant.YiliaoConstants;
import com.yiliao.common.core.error.CommonErrorCode;
import com.yiliao.common.core.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * AI 问答患者身份守卫（specs/modules/ai.md；AGENTS.md 患者数据权限不变量）。
 * 网关对患者路径的白名单只约束患者能访问什么，不拦截员工访问患者路径，
 * 因此入口侧校验必须在 ai-service 完成：
 * 1. 角色：仅 PATIENT 可用 AI 问答；
 * 2. 身份：X-User-Id 是 sys_user 的用户 id，必须经 patient 服务解析为 patient_archive.id，
 *    否则与档案 id 撞车时会读写他人患者数据（reportSymptom 是写操作）。
 * 校验顺序 角色→身份→档案解析 即安全边界，任何分支失败都拒绝，不静默放行。
 */
@Component
public class PatientChatGuard {

    private static final Logger log = LoggerFactory.getLogger(PatientChatGuard.class);

    private final PatientApi patientApi;

    public PatientChatGuard(PatientApi patientApi) {
        this.patientApi = patientApi;
    }

    /**
     * 在请求线程上执行：校验当前登录者为患者，并把用户 id 解析为本人档案 id。
     * 返回的档案 id 以不可变快照形式绑定给工具视图，供流式线程消费。
     */
    public Long requirePatientArchive() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes)) {
            // fail-closed：无请求上下文（后台线程/流式 worker）无法确认身份，按未认证拒绝，防止意外放行
            throw new BizException(CommonErrorCode.UNAUTHORIZED);
        }
        String roles = header(YiliaoConstants.HEADER_USER_ROLES);
        boolean patient = roles != null && Arrays.stream(roles.split(","))
                .map(String::trim)
                .anyMatch(YiliaoConstants.ROLE_PATIENT::equals);
        if (!patient) {
            throw new BizException(CommonErrorCode.FORBIDDEN.withMsg("AI 助手当前仅对患者端开放"));
        }
        Long userId = currentUserId();
        ArchiveDTO archive;
        try {
            archive = patientApi.getArchiveByUserId(userId).data();
        } catch (Exception e) {
            log.error("患者档案解析失败，拒绝访问 userId={}", userId, e);
            // 解析失败=拒绝（宁可拒绝不可放行）
            throw new BizException(CommonErrorCode.FORBIDDEN.withMsg("患者档案校验失败，请稍后再试"));
        }
        if (archive == null) {
            throw new BizException(CommonErrorCode.FORBIDDEN.withMsg("当前账号未建立患者档案"));
        }
        return archive.id();
    }

    private Long currentUserId() {
        String userId = header(YiliaoConstants.HEADER_USER_ID);
        if (userId == null || userId.isBlank()) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED);
        }
        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException e) {
            // 非法网关身份头视为未认证
            throw new BizException(CommonErrorCode.UNAUTHORIZED);
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
