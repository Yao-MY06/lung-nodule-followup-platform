package com.yiliao.nodule.feign;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiliao.api.nodule.NoduleApi;
import com.yiliao.api.nodule.dto.SnapshotDTO;
import com.yiliao.common.core.result.Result;
import com.yiliao.nodule.entity.ExamReport;
import com.yiliao.nodule.entity.Nodule;
import com.yiliao.nodule.entity.NoduleSnapshot;
import com.yiliao.nodule.mapper.ExamReportMapper;
import com.yiliao.nodule.mapper.NoduleMapper;
import com.yiliao.nodule.mapper.NoduleSnapshotMapper;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * NoduleApi 契约实现（specs/global/30 §5）。
 */
@RestController
public class NoduleApiController implements NoduleApi {

    private final NoduleMapper noduleMapper;
    private final NoduleSnapshotMapper snapshotMapper;
    private final ExamReportMapper reportMapper;

    public NoduleApiController(NoduleMapper noduleMapper, NoduleSnapshotMapper snapshotMapper,
                               ExamReportMapper reportMapper) {
        this.noduleMapper = noduleMapper;
        this.snapshotMapper = snapshotMapper;
        this.reportMapper = reportMapper;
    }

    @Override
    public Result<List<SnapshotDTO>> getTrend(Long patientId, Long noduleId) {
        List<Long> noduleIds;
        if (noduleId != null) {
            noduleIds = List.of(noduleId);
        } else {
            noduleIds = noduleMapper.selectList(new LambdaQueryWrapper<Nodule>()
                            .eq(Nodule::getPatientId, patientId))
                    .stream().map(Nodule::getId).toList();
        }
        if (noduleIds.isEmpty()) {
            return Result.ok(List.of());
        }
        List<NoduleSnapshot> snapshots = snapshotMapper.selectList(new LambdaQueryWrapper<NoduleSnapshot>()
                .in(NoduleSnapshot::getNoduleId, noduleIds)
                .orderByAsc(NoduleSnapshot::getExamDate));
        return Result.ok(snapshots.stream().map(NoduleApiController::toDTO).toList());
    }

    @Override
    public Result<String> reportSummary(Long patientId) {
        ExamReport latest = reportMapper.selectOne(new LambdaQueryWrapper<ExamReport>()
                .eq(ExamReport::getPatientId, patientId)
                .orderByDesc(ExamReport::getExamDate)
                .last("LIMIT 1"));
        return Result.ok(latest == null ? null : latest.getConclusion());
    }

    private static SnapshotDTO toDTO(NoduleSnapshot snapshot) {
        return new SnapshotDTO(snapshot.getNoduleId(), snapshot.getExamDate(),
                snapshot.getMaxDiameterMm(), snapshot.getSolidDiameterMm(),
                snapshot.getMeanDensityHu(), snapshot.getIsNew());
    }
}
