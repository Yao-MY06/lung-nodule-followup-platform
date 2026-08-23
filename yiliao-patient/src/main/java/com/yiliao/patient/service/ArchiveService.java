package com.yiliao.patient.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yiliao.api.followup.FollowupApi;
import com.yiliao.api.followup.dto.PlanGenerateRequest;
import com.yiliao.api.followup.dto.RuleInputDTO;
import com.yiliao.common.core.constant.YiliaoConstants;
import com.yiliao.common.core.exception.BizException;
import com.yiliao.common.core.page.PageQuery;
import com.yiliao.common.core.page.PageResult;
import com.yiliao.common.data.page.PageResults;
import com.yiliao.patient.crypto.IdCardCipher;
import com.yiliao.patient.dto.CreateArchiveRequest;
import com.yiliao.patient.entity.PatientArchive;
import com.yiliao.patient.entity.PatientDiagnosis;
import com.yiliao.patient.entity.PatientRiskFactor;
import com.yiliao.patient.error.PatientErrorCode;
import com.yiliao.patient.mapper.PatientArchiveMapper;
import com.yiliao.patient.mapper.PatientDiagnosisMapper;
import com.yiliao.patient.mapper.PatientRiskFactorMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 建档与档案管理（specs/modules/patient.md §4）。
 * 决策记录：建档→自动生成计划为同步 Feign 且失败不回滚（P4 引入 Seata AT 强化，见 flows/F1）。
 */
@Service
public class ArchiveService {

    private static final Logger log = LoggerFactory.getLogger(ArchiveService.class);
    private static final DateTimeFormatter NO = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 阶段状态机（specs/modules/patient.md §4.2；结案不可直接回退）。 */
    private static final Map<String, Set<String>> STAGE_TRANSITIONS = Map.of(
            PatientArchive.STAGE_NODULE, Set.of(PatientArchive.STAGE_POSTOP, PatientArchive.STAGE_TREATING,
                    PatientArchive.STAGE_MDT, PatientArchive.STAGE_LOST, PatientArchive.STAGE_CLOSED),
            PatientArchive.STAGE_POSTOP, Set.of(PatientArchive.STAGE_TREATING, PatientArchive.STAGE_LOST,
                    PatientArchive.STAGE_CLOSED),
            PatientArchive.STAGE_TREATING, Set.of(PatientArchive.STAGE_LOST, PatientArchive.STAGE_CLOSED),
            PatientArchive.STAGE_MDT, Set.of(PatientArchive.STAGE_NODULE, PatientArchive.STAGE_TREATING,
                    PatientArchive.STAGE_LOST, PatientArchive.STAGE_CLOSED),
            PatientArchive.STAGE_LOST, Set.of(PatientArchive.STAGE_NODULE, PatientArchive.STAGE_POSTOP,
                    PatientArchive.STAGE_CLOSED));

    private final PatientArchiveMapper archiveMapper;
    private final PatientRiskFactorMapper riskFactorMapper;
    private final PatientDiagnosisMapper diagnosisMapper;
    private final FollowupApi followupApi;
    private final IdCardCipher idCardCipher;
    private final RedissonClient redissonClient;

    public ArchiveService(PatientArchiveMapper archiveMapper, PatientRiskFactorMapper riskFactorMapper,
                          PatientDiagnosisMapper diagnosisMapper, FollowupApi followupApi,
                          IdCardCipher idCardCipher, RedissonClient redissonClient) {
        this.archiveMapper = archiveMapper;
        this.riskFactorMapper = riskFactorMapper;
        this.diagnosisMapper = diagnosisMapper;
        this.followupApi = followupApi;
        this.idCardCipher = idCardCipher;
        this.redissonClient = redissonClient;
    }

    public record CreateResult(Long archiveId, String patientNo, Long planId, String autoPlanError) {
    }

    public CreateResult create(CreateArchiveRequest request) {
        String lockKey = YiliaoConstants.LOCK_PREFIX + "patient:create:"
                + request.name() + ":" + (request.phone() == null ? "" : request.phone());
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(2, 15, TimeUnit.SECONDS)) {
                throw new BizException(PatientErrorCode.DUPLICATE_ARCHIVE);
            }
            return doCreate(request);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(PatientErrorCode.DUPLICATE_ARCHIVE.withMsg("建档被中断，请重试"));
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private CreateResult doCreate(CreateArchiveRequest request) {
        PatientArchive archive = new PatientArchive();
        archive.setPatientNo(generatePatientNo());
        archive.setName(request.name());
        archive.setGender(request.gender());
        archive.setBirthDate(request.birthDate());
        archive.setIdCard(idCardCipher.encrypt(request.idCard()));
        archive.setPhone(request.phone());
        archive.setAddress(request.address());
        archive.setEmergencyContact(request.emergencyContact());
        archive.setEmergencyPhone(request.emergencyPhone());
        archive.setDoctorId(request.doctorId());
        archive.setSourceType(request.sourceType());
        archive.setRemark(request.remark());
        archive.setStageLabel(PatientArchive.STAGE_NODULE);
        archiveMapper.insert(archive);

        if (request.riskFactor() != null) {
            PatientRiskFactor risk = new PatientRiskFactor();
            risk.setPatientId(archive.getId());
            risk.setSmokingPackYear(request.riskFactor().smokingPackYear());
            risk.setFamilyHistory(request.riskFactor().familyHistory());
            risk.setOccupationalExposure(request.riskFactor().occupationalExposure());
            risk.setPriorCancer(request.riskFactor().priorCancer());
            risk.setComorbidity(request.riskFactor().comorbidity());
            riskFactorMapper.insert(risk);
        }
        if (request.diagnosis() != null) {
            PatientDiagnosis diagnosis = new PatientDiagnosis();
            diagnosis.setPatientId(archive.getId());
            diagnosis.setTnmStage(request.diagnosis().tnmStage());
            diagnosis.setClinicalStage(request.diagnosis().clinicalStage());
            diagnosis.setPathologyType(request.diagnosis().pathologyType());
            diagnosis.setGeneResult(request.diagnosis().geneResult());
            diagnosis.setSurgeryDate(request.diagnosis().surgeryDate());
            diagnosis.setAdjuvantTherapy(request.diagnosis().adjuvantTherapy());
            diagnosis.setDiagnoseDate(request.diagnosis().diagnoseDate());
            diagnosisMapper.insert(diagnosis);
        }

        Long planId = null;
        String autoPlanError = null;
        if (request.plan() != null) {
            try {
                RuleInputDTO input = withRiskFromArchive(request.plan().input(), request);
                planId = followupApi.generatePlan(new PlanGenerateRequest(
                        archive.getId(), request.plan().scene(), input, null)).data();
            } catch (Exception e) {
                // 失败不阻断建档：留错误提示，由医生手动触发 plans/generate（P4 Seata 强化一致性）
                autoPlanError = e.getMessage();
                log.warn("建档后自动生成计划失败 archiveId={} msg={}", archive.getId(), e.getMessage());
            }
        }
        return new CreateResult(archive.getId(), archive.getPatientNo(), planId, autoPlanError);
    }

    /** 规则输入的 risk 因子：请求未显式提供时按危险因素档案推断。 */
    private RuleInputDTO withRiskFromArchive(RuleInputDTO input, CreateArchiveRequest request) {
        if (input.riskFactor() != null || request.riskFactor() == null) {
            return input;
        }
        PatientRiskFactor risk = new PatientRiskFactor();
        risk.setSmokingPackYear(request.riskFactor().smokingPackYear());
        risk.setFamilyHistory(request.riskFactor().familyHistory());
        risk.setOccupationalExposure(request.riskFactor().occupationalExposure());
        risk.setPriorCancer(request.riskFactor().priorCancer());
        return new RuleInputDTO(input.noduleType(), input.maxDiaMm(), risk.hasAnyRiskFactor(),
                input.stageGroup(), input.genePositive(), input.adjuvantTherapy());
    }

    public PageResult<PatientArchive> page(Long doctorId, String stageLabel, String keyword, PageQuery query) {
        return PageResults.of(archiveMapper.selectPage(
                Page.of(query.getPage(), query.getSize()),
                new LambdaQueryWrapper<PatientArchive>()
                        .eq(doctorId != null, PatientArchive::getDoctorId, doctorId)
                        .eq(stageLabel != null && !stageLabel.isBlank(), PatientArchive::getStageLabel, stageLabel)
                        .and(keyword != null && !keyword.isBlank(),
                                w -> w.like(PatientArchive::getName, keyword)
                                        .or().like(PatientArchive::getPatientNo, keyword))
                        .orderByDesc(PatientArchive::getId)));
    }

    public PatientArchive requireArchive(Long id) {
        PatientArchive archive = archiveMapper.selectById(id);
        if (archive == null) {
            throw new BizException(PatientErrorCode.ARCHIVE_NOT_FOUND);
        }
        return archive;
    }

    /** 变更阶段标签（状态机校验，specs/modules/patient.md §4.2）。 */
    public void changeStage(Long id, String target) {
        PatientArchive archive = requireArchive(id);
        Set<String> allowed = STAGE_TRANSITIONS.get(archive.getStageLabel());
        if (allowed == null || !allowed.contains(target)) {
            throw new BizException(PatientErrorCode.STAGE_TRANSITION_ILLEGAL.withMsg(
                    archive.getStageLabel() + " → " + target + " 不允许"));
        }
        archive.setStageLabel(target);
        archiveMapper.updateById(archive);
    }

    public void changeDoctor(Long id, Long doctorId) {
        PatientArchive archive = requireArchive(id);
        archive.setDoctorId(doctorId);
        archiveMapper.updateById(archive);
    }

    /** 患者端：按登录 userId 查本人档案（服务端强制本人数据，specs/modules/patient.md §6）。 */
    public PatientArchive findMine(Long userId) {
        return archiveMapper.selectOne(new LambdaQueryWrapper<PatientArchive>()
                .eq(PatientArchive::getUserId, userId)
                .last("LIMIT 1"));
    }

    /** 是否存在任一危险因素（PatientApi.hasRiskFactor 契约，F1 确认链路使用）。 */
    public boolean hasRiskFactor(Long patientId) {
        PatientRiskFactor risk = riskFactorMapper.selectOne(new LambdaQueryWrapper<PatientRiskFactor>()
                .eq(PatientRiskFactor::getPatientId, patientId));
        return risk != null && risk.hasAnyRiskFactor();
    }

    /** 出参脱敏（specs/global/10 §9）：身份证解密后脱敏、手机号脱敏。 */
    public MaskedArchiveVO detailMasked(Long id) {
        PatientArchive archive = requireArchive(id);
        PatientRiskFactor risk = riskFactorMapper.selectOne(new LambdaQueryWrapper<PatientRiskFactor>()
                .eq(PatientRiskFactor::getPatientId, id));
        return new MaskedArchiveVO(
                archive.getId(), archive.getPatientNo(), archive.getName(), archive.getGender(),
                archive.getBirthDate(), IdCardCipher.mask(idCardCipher.decrypt(archive.getIdCard())),
                IdCardCipher.maskPhone(archive.getPhone()), archive.getAddress(),
                archive.getEmergencyContact(), IdCardCipher.maskPhone(archive.getEmergencyPhone()),
                archive.getDoctorId(), archive.getStageLabel(), archive.getSourceType(), archive.getRemark(),
                risk == null ? null : new RiskFactorVO(risk.getSmokingPackYear(), risk.getFamilyHistory(),
                        risk.getOccupationalExposure(), risk.getPriorCancer(), risk.getComorbidity()));
    }

    private String generatePatientNo() {
        return "P" + LocalDate.now().format(NO) + "-" + String.format("%04d",
                ThreadLocalRandom.current().nextInt(10000));
    }

    public record MaskedArchiveVO(Long id, String patientNo, String name, Integer gender,
                                  LocalDate birthDate, String idCardMasked, String phoneMasked,
                                  String address, String emergencyContact, String emergencyPhoneMasked,
                                  Long doctorId, String stageLabel, Integer sourceType, String remark,
                                  RiskFactorVO riskFactor) {
    }

    public record RiskFactorVO(java.math.BigDecimal smokingPackYear, Integer familyHistory,
                               Integer occupationalExposure, Integer priorCancer, String comorbidity) {
    }
}
