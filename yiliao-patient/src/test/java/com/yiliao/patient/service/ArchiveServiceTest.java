package com.yiliao.patient.service;

import com.yiliao.api.followup.FollowupApi;
import com.yiliao.api.followup.dto.RuleInputDTO;
import com.yiliao.patient.crypto.IdCardCipher;
import com.yiliao.patient.dto.CreateArchiveRequest;
import com.yiliao.patient.entity.PatientArchive;
import com.yiliao.patient.error.PatientErrorCode;
import com.yiliao.patient.mapper.PatientArchiveMapper;
import com.yiliao.patient.mapper.PatientDiagnosisMapper;
import com.yiliao.patient.mapper.PatientRiskFactorMapper;
import com.yiliao.common.core.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArchiveServiceTest {

    /** 与 application.yml 开发占位一致的 32 字节密钥 */
    private static final String KEY = "ZGV2LW9ubHktaWRjYXJkLWtleS0wMTIzNDU2Nzg5YWI=";

    @Mock
    private PatientArchiveMapper archiveMapper;
    @Mock
    private PatientRiskFactorMapper riskFactorMapper;
    @Mock
    private PatientDiagnosisMapper diagnosisMapper;
    @Mock
    private FollowupApi followupApi;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;

    private ArchiveService service;
    private final IdCardCipher cipher = new IdCardCipher(KEY);

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(redissonClient.getLock(anyString())).thenReturn(lock);
        lenient().when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        lenient().when(lock.isHeldByCurrentThread()).thenReturn(true);
        service = new ArchiveService(archiveMapper, riskFactorMapper, diagnosisMapper,
                followupApi, cipher, redissonClient);
    }

    private CreateArchiveRequest request(CreateArchiveRequest.PlanRequest plan) {
        return request("110101196503120011", plan);
    }

    private CreateArchiveRequest request(String idCard, CreateArchiveRequest.PlanRequest plan) {
        return new CreateArchiveRequest("张三", 1, null, idCard, "13812345678",
                null, null, null, 2L, 1, null,
                new CreateArchiveRequest.RiskFactorRequest(new BigDecimal("30"), 1, 0, 0, null),
                null, plan);
    }

    @Test
    void lockKeyUsesNormalizedIdCardHash() {
        service.create(request(" 11010119650312001x ", null));
        service.create(request("11010119650312001X", null));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redissonClient, times(2)).getLock(captor.capture());
        assertEquals(captor.getAllValues().get(0), captor.getAllValues().get(1));
        assertTrue(captor.getAllValues().get(0).matches(".*:[0-9a-f]{64}$"));
        assertFalse(captor.getAllValues().get(0).contains("11010119650312001"));
    }

    @Test
    void differentIdCardsUseDifferentLockKeys() {
        service.create(request("110101196503120011", null));
        service.create(request("110101196503120012", null));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redissonClient, times(2)).getLock(captor.capture());
        assertNotEquals(captor.getAllValues().get(0), captor.getAllValues().get(1));
    }

    @Test
    void blankIdCardsDoNotShareLockKey() {
        service.create(request("   ", null));
        service.create(request(null, null));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redissonClient, times(2)).getLock(captor.capture());
        assertNotEquals(captor.getAllValues().get(0), captor.getAllValues().get(1));
    }

    @Test
    void existingIdCardHashIsRejectedBeforeInsert() {
        when(archiveMapper.selectOne(any())).thenReturn(new PatientArchive());

        BizException e = assertThrows(BizException.class,
                () -> service.create(request("110101196503120011", null)));

        assertEquals(PatientErrorCode.DUPLICATE_ARCHIVE.getCode(), e.getCode());
        verify(archiveMapper, never()).insert(any(PatientArchive.class));
    }

    @Test
    void databaseUniqueConflictIsRejectedAsDuplicate() {
        when(archiveMapper.insert(any(PatientArchive.class)))
                .thenThrow(new DuplicateKeyException("duplicate id_card_hash"));

        BizException e = assertThrows(BizException.class,
                () -> service.create(request("110101196503120011", null)));

        assertEquals(PatientErrorCode.DUPLICATE_ARCHIVE.getCode(), e.getCode());
    }

    @Test
    void createEncryptsIdCardAndAutoGeneratesPlan() {
        when(followupApi.generatePlan(any()))
                .thenReturn(com.yiliao.common.core.result.Result.ok(55L));

        ArchiveService.CreateResult result = service.create(request(
                new CreateArchiveRequest.PlanRequest(1,
                        new RuleInputDTO(3, new BigDecimal("4.0"), null, null, null, null))));

        assertEquals(55L, result.planId());
        assertNull(result.autoPlanError());
        ArgumentCaptor<PatientArchive> captor = ArgumentCaptor.forClass(PatientArchive.class);
        verify(archiveMapper).insert(captor.capture());
        // 落库的是密文，且可解密还原（specs/modules/patient.md §4.3）
        assertNotEquals("110101196503120011", captor.getValue().getIdCard());
        assertEquals("110101196503120011", cipher.decrypt(captor.getValue().getIdCard()));
        assertEquals(IdCardCipher.hash("110101196503120011"), captor.getValue().getIdCardHash());
        assertEquals(PatientArchive.STAGE_NODULE, captor.getValue().getStageLabel());
    }

    @Test
    void riskFactorInferredFromArchiveWhenPlanInputOmitsIt() {
        when(followupApi.generatePlan(any())).thenReturn(com.yiliao.common.core.result.Result.ok(55L));
        service.create(request(new CreateArchiveRequest.PlanRequest(1,
                new RuleInputDTO(3, new BigDecimal("4.0"), null, null, null, null))));

        ArgumentCaptor<com.yiliao.api.followup.dto.PlanGenerateRequest> captor =
                ArgumentCaptor.forClass(com.yiliao.api.followup.dto.PlanGenerateRequest.class);
        verify(followupApi).generatePlan(captor.capture());
        // 危险因素档案有吸烟史+家族史 → risk=true 推断
        assertTrue(Boolean.TRUE.equals(captor.getValue().input().riskFactor()));
    }

    @Test
    void planFailureDoesNotBlockArchiveCreation() {
        when(followupApi.generatePlan(any())).thenThrow(new RuntimeException("followup down"));

        ArchiveService.CreateResult result = service.create(request(
                new CreateArchiveRequest.PlanRequest(1,
                        new RuleInputDTO(3, new BigDecimal("4.0"), null, null, null, null))));

        assertNull(result.planId());
        assertTrue(result.autoPlanError() != null);
        verify(archiveMapper).insert(any(PatientArchive.class));
    }

    @Test
    void lockContentionRejectsAsDuplicate() throws Exception {
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(false);

        BizException e = assertThrows(BizException.class, () -> service.create(request(null)));
        assertEquals(PatientErrorCode.DUPLICATE_ARCHIVE.getCode(), e.getCode());
    }

    @Test
    void noPlanRequestSkipsFollowupCall() {
        service.create(request(null));
        verify(followupApi, org.mockito.Mockito.never()).generatePlan(any());
    }

    @Test
    void stageTransitionStateMachine() {
        PatientArchive archive = new PatientArchive();
        archive.setId(1L);
        archive.setStageLabel(PatientArchive.STAGE_NODULE);
        when(archiveMapper.selectById(1L)).thenReturn(archive);

        service.changeStage(1L, PatientArchive.STAGE_MDT);

        archive.setStageLabel(PatientArchive.STAGE_CLOSED);
        when(archiveMapper.selectById(1L)).thenReturn(archive);
        BizException e = assertThrows(BizException.class,
                () -> service.changeStage(1L, PatientArchive.STAGE_NODULE));
        assertEquals(PatientErrorCode.STAGE_TRANSITION_ILLEGAL.getCode(), e.getCode());
    }
}
