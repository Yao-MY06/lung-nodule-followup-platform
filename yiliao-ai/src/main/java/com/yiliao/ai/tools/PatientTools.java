package com.yiliao.ai.tools;

import com.yiliao.api.followup.FollowupApi;
import com.yiliao.api.followup.dto.NextFollowupDTO;
import com.yiliao.api.nodule.NoduleApi;
import com.yiliao.api.nodule.dto.SnapshotDTO;
import com.yiliao.common.core.constant.YiliaoConstants;
import com.yiliao.common.core.exception.BizException;
import com.yiliao.common.core.error.CommonErrorCode;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * F4 工具注册（specs/flows/F4）：LLM 可调用的四个真实能力。
 * 安全底线：patientId 一律取网关透传的 X-User-Id（当前登录患者），
 * 工具方法**不接受也不信任** LLM 提供的任何 patientId——越权在 Java 层拦截，不靠 Prompt。
 */
@Component
public class PatientTools {

    private final FollowupApi followupApi;
    private final NoduleApi noduleApi;
    private final Long patientIdSnapshot;
    private final boolean identityBound;

    /** 多构造器场景必须显式指定注入构造器（2026-09-05 T4 回归发现：无标注时 Spring 回退无参构造 → 启动失败）。 */
    @Autowired
    public PatientTools(FollowupApi followupApi, NoduleApi noduleApi) {
        this(followupApi, noduleApi, null, false);
    }

    private PatientTools(FollowupApi followupApi, NoduleApi noduleApi,
                         Long patientIdSnapshot, boolean identityBound) {
        this.followupApi = followupApi;
        this.noduleApi = noduleApi;
        this.patientIdSnapshot = patientIdSnapshot;
        this.identityBound = identityBound;
    }

    /**
     * Create a request-scoped tool view with an immutable patient identity.
     * The view is used by streaming calls, where request thread locals are not
     * available on the Reactor worker that executes a tool.
     */
    public PatientTools forPatient(Long patientId) {
        return new PatientTools(followupApi, noduleApi, patientId, true);
    }

    @Tool(description = "查询当前患者本人的下次随访计划（日期与项目）")
    public NextFollowupDTO queryNextFollowup() {
        Long patientId = resolvePatientId();
        return followupApi.nextFollowup(patientId).data();
    }

    @Tool(description = "查询当前患者本人的结节变化趋势（历次复查快照）")
    public List<SnapshotDTO> queryNoduleTrend() {
        Long patientId = resolvePatientId();
        return noduleApi.getTrend(patientId, null).data();
    }

    @Tool(description = "上报症状。symptom：症状名；severity：严重程度 1-5")
    public String reportSymptom(@ToolParam(description = "症状，如 咳嗽/疼痛/乏力") String symptom,
                                @ToolParam(description = "严重程度 1-5 的整数") int severity) {
        Long patientId = resolvePatientId();
        if (severity < 1 || severity > 5) {
            return "severity 必须在 1-5 之间";
        }
        Long id = followupApi.reportSymptom(patientId,
                new FollowupApi.SymptomRequest(symptom, severity, "AI 助手上报", 2)).data();
        return severity >= 3
                ? "症状已上报（编号 " + id + "），严重程度较高，已通知您的医生关注。"
                : "症状已上报（编号 " + id + "）。";
    }

    @Tool(description = "查询当前患者本人最近一次检查报告的结论摘要")
    public String queryReportSummary() {
        Long patientId = resolvePatientId();
        String summary = noduleApi.reportSummary(patientId).data();
        return summary == null ? "暂无检查报告记录" : summary;
    }

    private Long resolvePatientId() {
        if (identityBound) {
            if (patientIdSnapshot == null) {
                throw new BizException(CommonErrorCode.UNAUTHORIZED);
            }
            return patientIdSnapshot;
        }
        return currentPatientId();
    }

    /** 当前登录患者 id：只认网关透传头，LLM 无法注入。 */
    static Long currentPatientId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String userId = attributes.getRequest().getHeader(YiliaoConstants.HEADER_USER_ID);
            if (userId != null && !userId.isBlank()) {
                try {
                    return Long.valueOf(userId);
                } catch (NumberFormatException ignored) {
                    // Treat malformed gateway identity headers as unauthenticated.
                }
            }
        }
        throw new BizException(CommonErrorCode.UNAUTHORIZED);
    }
}
