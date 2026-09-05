package com.yiliao.ai.security;

import com.yiliao.api.patient.PatientApi;
import com.yiliao.api.patient.dto.ArchiveDTO;
import com.yiliao.common.core.constant.YiliaoConstants;
import com.yiliao.common.core.exception.BizException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 患者身份守卫单测：校验顺序 角色→身份→档案解析，任何分支失败都拒绝。
 */
@ExtendWith(MockitoExtension.class)
class PatientChatGuardTest {

    @Mock
    private PatientApi patientApi;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void rejectsNonPatientRoleWithoutTouchingPatientApi() {
        setRequest("DOCTOR,ADMIN", "5001");
        PatientChatGuard guard = new PatientChatGuard(patientApi);

        BizException exception = assertThrows(BizException.class, guard::requirePatientArchive);

        assertEquals(403, exception.getCode());
        assertEquals("AI 助手当前仅对患者端开放", exception.getMessage());
        verify(patientApi, never()).getArchiveByUserId(any());
    }

    @Test
    void resolvesPatientArchiveIdForPatientRole() {
        setRequest(YiliaoConstants.ROLE_PATIENT, "77");
        when(patientApi.getArchiveByUserId(77L)).thenReturn(com.yiliao.common.core.result.Result.ok(
                new ArchiveDTO(1009L, null, null, null, null, null)));
        PatientChatGuard guard = new PatientChatGuard(patientApi);

        assertEquals(1009L, guard.requirePatientArchive());
    }

    @Test
    void rejectsPatientWithoutArchive() {
        setRequest(YiliaoConstants.ROLE_PATIENT, "77");
        when(patientApi.getArchiveByUserId(77L)).thenReturn(com.yiliao.common.core.result.Result.ok(null));
        PatientChatGuard guard = new PatientChatGuard(patientApi);

        BizException exception = assertThrows(BizException.class, guard::requirePatientArchive);

        assertEquals(403, exception.getCode());
        assertEquals("当前账号未建立患者档案", exception.getMessage());
    }

    @Test
    void rejectsWhenArchiveLookupFails() {
        setRequest(YiliaoConstants.ROLE_PATIENT, "77");
        when(patientApi.getArchiveByUserId(77L)).thenThrow(new RuntimeException("feign down"));
        PatientChatGuard guard = new PatientChatGuard(patientApi);

        BizException exception = assertThrows(BizException.class, guard::requirePatientArchive);

        assertEquals(403, exception.getCode());
        assertEquals("患者档案校验失败，请稍后再试", exception.getMessage());
    }

    @Test
    void rejectsWhenRequestContextIsMissing() {
        RequestContextHolder.resetRequestAttributes();
        PatientChatGuard guard = new PatientChatGuard(patientApi);

        BizException exception = assertThrows(BizException.class, guard::requirePatientArchive);

        assertEquals(401, exception.getCode());
        verify(patientApi, never()).getArchiveByUserId(any());
    }

    @Test
    void rejectsMalformedUserId() {
        setRequest(YiliaoConstants.ROLE_PATIENT, "abc");
        PatientChatGuard guard = new PatientChatGuard(patientApi);

        BizException exception = assertThrows(BizException.class, guard::requirePatientArchive);

        assertEquals(401, exception.getCode());
        verify(patientApi, never()).getArchiveByUserId(any());
    }

    private static void setRequest(String roles, String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(YiliaoConstants.HEADER_USER_ROLES, roles);
        request.addHeader(YiliaoConstants.HEADER_USER_ID, userId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
