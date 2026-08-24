package com.yiliao.patient.security;

import com.yiliao.common.core.exception.BizException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PatientRoleGuardTest {

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

    @Test
    void patientRejectedFromStaffActions() {
        mockRequest(88L, "PATIENT");
        BizException e = assertThrows(BizException.class, PatientRoleGuard::requireStaff);
        assertEquals(403, e.getCode());
    }

    @Test
    void patientSeesOwnArchive() {
        mockRequest(88L, "PATIENT");
        assertDoesNotThrow(() -> PatientRoleGuard.requireOwnArchive(88L));
    }

    @Test
    void patientSeesOthersArchiveRejected() {
        mockRequest(88L, "PATIENT");
        assertThrows(BizException.class, () -> PatientRoleGuard.requireOwnArchive(99L));
    }

    @Test
    void staffPassesAll() {
        mockRequest(7L, "DOCTOR");
        assertDoesNotThrow(PatientRoleGuard::requireStaff);
        assertDoesNotThrow(() -> PatientRoleGuard.requireOwnArchive(999L));
    }

    @Test
    void systemContextPasses() {
        assertDoesNotThrow(PatientRoleGuard::requireStaff);
        assertDoesNotThrow(() -> PatientRoleGuard.requireOwnArchive(999L));
    }
}
