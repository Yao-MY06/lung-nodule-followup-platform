package com.yiliao.ai.tools;

import com.yiliao.api.followup.FollowupApi;
import com.yiliao.api.followup.dto.NextFollowupDTO;
import com.yiliao.api.nodule.NoduleApi;
import com.yiliao.common.core.exception.BizException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientToolsTest {

    @Mock
    private FollowupApi followupApi;
    @Mock
    private NoduleApi noduleApi;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "1001");   // 当前登录患者
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void queryNextFollowupUsesCurrentUserNotLlmSupplied() {
        when(followupApi.nextFollowup(1001L)).thenReturn(com.yiliao.common.core.result.Result.ok(
                new NextFollowupDTO(9L, LocalDate.of(2026, 9, 10), "[胸部CT]", 1)));

        PatientTools tools = new PatientTools(followupApi, noduleApi);
        // 工具方法签名不含 patientId——LLM 无法传参越权（F4 验证清单 #2）
        NextFollowupDTO result = tools.queryNextFollowup();

        assertEquals(LocalDate.of(2026, 9, 10), result.planDate());
        verify(followupApi).nextFollowup(eq(1001L));
    }

    @Test
    void reportSymptomValidatesSeverityRange() {
        PatientTools tools = new PatientTools(followupApi, noduleApi);
        String result = tools.reportSymptom("咳嗽", 9);
        assertEquals("severity 必须在 1-5 之间", result);
    }

    @Test
    void reportSymptomHighSeverityNotifiesDoctor() {
        when(followupApi.reportSymptom(eq(1001L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(com.yiliao.common.core.result.Result.ok(77L));

        PatientTools tools = new PatientTools(followupApi, noduleApi);
        String result = tools.reportSymptom("咳嗽", 3);

        org.junit.jupiter.api.Assertions.assertTrue(result.contains("已通知"));
        verify(followupApi).reportSymptom(eq(1001L), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void noUserContextRejected() {
        RequestContextHolder.resetRequestAttributes();
        PatientTools tools = new PatientTools(followupApi, noduleApi);
        assertThrows(BizException.class, tools::queryNextFollowup);
    }
}
