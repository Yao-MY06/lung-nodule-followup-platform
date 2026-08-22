package com.yiliao.nodule.confirm;

import com.yiliao.api.followup.FollowupApi;
import com.yiliao.api.patient.PatientApi;
import com.yiliao.nodule.entity.ExamReport;
import com.yiliao.nodule.entity.Nodule;
import com.yiliao.nodule.entity.NoduleSnapshot;
import com.yiliao.nodule.error.NoduleErrorCode;
import com.yiliao.nodule.mapper.ExamReportMapper;
import com.yiliao.nodule.mapper.NoduleMapper;
import com.yiliao.nodule.mapper.NoduleSnapshotMapper;
import com.yiliao.common.core.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmServiceTest {

    private static final String STRUCTURED_JSON = """
            {"location":"右肺上叶","noduleType":"PURE_GG","maxDiameterMm":4.0,
             "solidDiameterMm":null,"count":1,"signs":["毛刺"],"impression":"磨玻璃结节"}
            """;

    @Mock
    private ExamReportMapper reportMapper;
    @Mock
    private NoduleMapper noduleMapper;
    @Mock
    private NoduleSnapshotMapper snapshotMapper;
    @Mock
    private PatientApi patientApi;
    @Mock
    private FollowupApi followupApi;

    private ExamReport report() {
        ExamReport report = new ExamReport();
        report.setId(9001L);
        report.setPatientId(1001L);
        report.setExamDate(LocalDate.of(2026, 8, 22));
        report.setStructuredJson(STRUCTURED_JSON);
        report.setExtractStatus(1);
        return report;
    }

    @Test
    void confirmCreatesNoduleSnapshotAndPlan() {
        ExamReport report = report();
        when(reportMapper.selectById(9001L)).thenReturn(report);
        when(noduleMapper.selectCount(any())).thenReturn(0L);
        when(noduleMapper.insert(any(Nodule.class))).thenAnswer(inv -> {
            inv.getArgument(0, Nodule.class).setId(101L);
            return 1;
        });
        when(snapshotMapper.insert(any(NoduleSnapshot.class))).thenAnswer(inv -> {
            inv.getArgument(0, NoduleSnapshot.class).setId(501L);
            return 1;
        });
        when(patientApi.hasRiskFactor(1001L))
                .thenReturn(com.yiliao.common.core.result.Result.ok(false));
        when(followupApi.generatePlan(any()))
                .thenReturn(com.yiliao.common.core.result.Result.ok(66L));

        ConfirmService service = new ConfirmService(reportMapper, noduleMapper, snapshotMapper,
                patientApi, followupApi);
        ConfirmService.ConfirmResult result = service.confirm(9001L, false, 0);

        assertEquals(101L, result.noduleId());
        assertEquals(66L, result.planId());
        assertEquals(2, report.getExtractStatus());   // 1→2 已确认

        ArgumentCaptor<Nodule> noduleCaptor = ArgumentCaptor.forClass(Nodule.class);
        verify(noduleMapper).insert(noduleCaptor.capture());
        assertEquals("N1", noduleCaptor.getValue().getNoduleNo());
        assertEquals(3, noduleCaptor.getValue().getNoduleType());   // PURE_GG→3

        // 规则输入携带真实 risk（PatientApi 取得）
        ArgumentCaptor<com.yiliao.api.followup.dto.PlanGenerateRequest> planCaptor =
                ArgumentCaptor.forClass(com.yiliao.api.followup.dto.PlanGenerateRequest.class);
        verify(followupApi).generatePlan(planCaptor.capture());
        assertEquals(Boolean.FALSE, planCaptor.getValue().input().riskFactor());
        assertEquals(new BigDecimal("4.0"), planCaptor.getValue().input().maxDiaMm());
    }

    @Test
    void planFailureDoesNotRollbackConfirm() {
        ExamReport report = report();
        when(reportMapper.selectById(9001L)).thenReturn(report);
        when(noduleMapper.selectCount(any())).thenReturn(0L);
        when(noduleMapper.insert(any(Nodule.class))).thenAnswer(inv -> {
            inv.getArgument(0, Nodule.class).setId(101L);
            return 1;
        });
        when(snapshotMapper.insert(any(NoduleSnapshot.class))).thenReturn(1);
        when(patientApi.hasRiskFactor(1001L))
                .thenReturn(com.yiliao.common.core.result.Result.ok(false));
        when(followupApi.generatePlan(any())).thenThrow(new RuntimeException("followup down"));

        ConfirmService service = new ConfirmService(reportMapper, noduleMapper, snapshotMapper,
                patientApi, followupApi);
        ConfirmService.ConfirmResult result = service.confirm(9001L, false, 0);

        assertEquals(2, report.getExtractStatus());
        assertEquals(null, result.planId());
        org.junit.jupiter.api.Assertions.assertNotNull(result.planError());
    }

    @Test
    void doubleConfirmRejected() {
        ExamReport report = report();
        report.setExtractStatus(2);
        when(reportMapper.selectById(9001L)).thenReturn(report);

        ConfirmService service = new ConfirmService(reportMapper, noduleMapper, snapshotMapper,
                patientApi, followupApi);
        BizException e = assertThrows(BizException.class, () -> service.confirm(9001L, false, 0));
        assertEquals(NoduleErrorCode.ALREADY_CONFIRMED.getCode(), e.getCode());
    }

    @Test
    void missingStructuredJsonRejected() {
        ExamReport report = report();
        report.setStructuredJson(null);
        when(reportMapper.selectById(9001L)).thenReturn(report);

        ConfirmService service = new ConfirmService(reportMapper, noduleMapper, snapshotMapper,
                patientApi, followupApi);
        BizException e = assertThrows(BizException.class, () -> service.confirm(9001L, false, 0));
        assertEquals(NoduleErrorCode.STRUCTURED_JSON_INVALID.getCode(), e.getCode());
    }
}
