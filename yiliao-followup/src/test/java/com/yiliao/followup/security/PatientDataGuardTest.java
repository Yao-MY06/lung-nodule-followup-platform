package com.yiliao.followup.security;

import com.yiliao.api.patient.PatientApi;
import com.yiliao.api.patient.dto.ArchiveDTO;
import com.yiliao.common.core.exception.BizException;
import com.yiliao.common.core.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * 患者数据归属守卫单测（红区：越权校验是 AGENTS.md 不变量的实现证据）。
 */
@ExtendWith(MockitoExtension.class)
class PatientDataGuardTest {

    @Mock
    private PatientApi patientApi;

    private void mockRequest(Long userId, String roles) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", String.valueOf(userId));
        request.addHeader("X-User-Roles", roles);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private ArchiveDTO archive(Long id) {
        return new ArchiveDTO(id, "P1", "张三", "结节随访中", 2L, 1);
    }

    @Test
    void patientAccessOwnDataPasses() {
        mockRequest(88L, "PATIENT");
        when(patientApi.getArchiveByUserId(88L)).thenReturn(Result.ok(archive(1001L)));

        PatientDataGuard guard = new PatientDataGuard(patientApi);
        assertDoesNotThrow(() -> guard.requireOwnOrStaff(1001L));
    }

    @Test
    void patientAccessOthersDataRejected403() {
        // 越权用例：患者调他人计划（specs 验证清单）
        mockRequest(88L, "PATIENT");
        when(patientApi.getArchiveByUserId(88L)).thenReturn(Result.ok(archive(1001L)));

        PatientDataGuard guard = new PatientDataGuard(patientApi);
        BizException e = assertThrows(BizException.class, () -> guard.requireOwnOrStaff(1002L));
        assertEquals(403, e.getCode());
    }

    @Test
    void patientWithoutArchiveRejected() {
        mockRequest(88L, "PATIENT");
        when(patientApi.getArchiveByUserId(88L)).thenReturn(Result.ok(null));

        PatientDataGuard guard = new PatientDataGuard(patientApi);
        assertThrows(BizException.class, () -> guard.requireOwnOrStaff(1001L));
    }

    @Test
    void archiveResolutionFailureFailsClosed() {
        // 宁可拒绝不可放行：PatientApi 异常时患者请求被拒绝
        mockRequest(88L, "PATIENT");
        when(patientApi.getArchiveByUserId(88L)).thenThrow(new RuntimeException("patient-service down"));

        PatientDataGuard guard = new PatientDataGuard(patientApi);
        assertThrows(BizException.class, () -> guard.requireOwnOrStaff(1001L));
    }

    @Test
    void staffPassesWithoutPatientApiCall() {
        mockRequest(7L, "DOCTOR");
        PatientDataGuard guard = new PatientDataGuard(patientApi);
        assertDoesNotThrow(() -> guard.requireOwnOrStaff(9999L));
        assertDoesNotThrow(guard::requireStaff);
    }

    @Test
    void patientRejectedFromStaffEndpoint() {
        mockRequest(88L, "PATIENT");
        PatientDataGuard guard = new PatientDataGuard(patientApi);
        assertThrows(BizException.class, guard::requireStaff);
    }

    @Test
    void systemContextPasses() {
        // 无请求上下文（XXL-Job/MQ）放行
        PatientDataGuard guard = new PatientDataGuard(patientApi);
        assertDoesNotThrow(() -> guard.requireOwnOrStaff(1001L));
        assertDoesNotThrow(guard::requireStaff);
    }
}
