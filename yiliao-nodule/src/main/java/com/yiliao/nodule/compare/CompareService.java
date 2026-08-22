package com.yiliao.nodule.compare;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiliao.nodule.entity.NoduleSnapshot;
import com.yiliao.nodule.mapper.NoduleSnapshotMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 快照对比（specs/modules/nodule.md §4.1，flows/F3）：
 * 任一径或实性成分增大 ≥2mm 判进展。nodule 只算 Δ，升级策略归 followup。
 * YILIAO_NODULE_PROGRESS 事件发布在 P3 接入 MQ 后补充。
 */
@Service
public class CompareService {

    public static final BigDecimal PROGRESS_THRESHOLD_MM = new BigDecimal("2.0");

    private final NoduleSnapshotMapper snapshotMapper;

    public CompareService(NoduleSnapshotMapper snapshotMapper) {
        this.snapshotMapper = snapshotMapper;
    }

    /** 最近两次快照对比；不足两条返回"无基线"。 */
    public CompareVO compare(Long noduleId) {
        List<NoduleSnapshot> latest = snapshotMapper.selectList(new LambdaQueryWrapper<NoduleSnapshot>()
                .eq(NoduleSnapshot::getNoduleId, noduleId)
                .orderByDesc(NoduleSnapshot::getExamDate)
                .orderByDesc(NoduleSnapshot::getId)
                .last("LIMIT 2"));

        if (latest.size() < 2) {
            NoduleSnapshot current = latest.isEmpty() ? null : latest.get(0);
            return new CompareVO(noduleId, current == null ? null : current.getExamDate(), null,
                    null, null, null, false,
                    current != null && Integer.valueOf(1).equals(current.getIsNew()),
                    "无基线快照，本次仅建立基线");
        }

        NoduleSnapshot current = latest.get(0);
        NoduleSnapshot previous = latest.get(1);
        BigDecimal deltaMax = delta(current.getMaxDiameterMm(), previous.getMaxDiameterMm());
        BigDecimal deltaSolid = delta(current.getSolidDiameterMm(), previous.getSolidDiameterMm());
        Integer deltaDensity = current.getMeanDensityHu() != null && previous.getMeanDensityHu() != null
                ? current.getMeanDensityHu() - previous.getMeanDensityHu() : null;

        boolean progress = ge(deltaMax, PROGRESS_THRESHOLD_MM) || ge(deltaSolid, PROGRESS_THRESHOLD_MM);
        boolean isNew = Integer.valueOf(1).equals(current.getIsNew());

        String advice;
        if (progress) {
            advice = "结节增大或实性成分增加≥2mm，建议升级随访策略并通知主管医生";
        } else if (isNew) {
            advice = "新发结节，请主管医生评估随访策略";
        } else {
            advice = "结节稳定，按原计划继续随访";
        }
        return new CompareVO(noduleId, current.getExamDate(), previous.getExamDate(),
                deltaMax, deltaSolid, deltaDensity, progress, isNew, advice);
    }

    /** 纵向趋势（升序，前端折线图）。 */
    public List<NoduleSnapshot> trend(Long noduleId) {
        return snapshotMapper.selectList(new LambdaQueryWrapper<NoduleSnapshot>()
                .eq(NoduleSnapshot::getNoduleId, noduleId)
                .orderByAsc(NoduleSnapshot::getExamDate));
    }

    private static BigDecimal delta(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null) {
            return null;
        }
        return current.subtract(previous);
    }

    private static boolean ge(BigDecimal value, BigDecimal threshold) {
        return value != null && value.compareTo(threshold) >= 0;
    }

    /**
     * 对比结果：deltas 为 null 表示该维度数据缺失。
     */
    public record CompareVO(
            Long noduleId,
            LocalDate currentExamDate,
            LocalDate previousExamDate,
            BigDecimal deltaMaxMm,
            BigDecimal deltaSolidMm,
            Integer deltaDensityHu,
            boolean progress,
            boolean newNodule,
            String advice
    ) {
    }
}
