package com.yiliao.statistics.controller;

import com.yiliao.common.core.result.Result;
import com.yiliao.statistics.entity.StatsFollowupMonthly;
import com.yiliao.statistics.service.StatsAggregateService;
import com.yiliao.statistics.service.StatsCalculator;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 统计驾驶舱接口（specs/modules/statistics.md §3）：四个只读 GET，全部走本库快照表。
 * 空表语义：overview 返回零值 + 提示；列表接口返回空集合（specs §5）。
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private static final String EMPTY_HINT = "暂无快照数据，等待统计任务 statsAggregateJob 首次执行";

    private final StatsAggregateService statsService;

    public StatsController(StatsAggregateService statsService) {
        this.statsService = statsService;
    }

    @Operation(summary = "驾驶舱总览：在管人数/进行中计划/逾期任务/预警数（含数据更新时间提示）")
    @GetMapping("/overview")
    public Result<OverviewVO> overview() {
        var daily = statsService.overview();
        // updateTime 非空=快照已入库；空表零值对象 updateTime=null → 返回提示（specs §5「数据更新于」）
        String updatedAt = daily.getUpdateTime() != null
                ? "数据更新于 " + daily.getUpdateTime() : EMPTY_HINT;
        return Result.ok(new OverviewVO(daily.getManagingCount(), daily.getActivePlanCount(),
                daily.getOverdueTaskCount(), daily.getAlertCount(), updatedAt));
    }

    @Operation(summary = "结节分布：性质(nodule_type)/风险分层(risk_level) 计数")
    @GetMapping("/nodule-distribution")
    public Result<List<DistItemVO>> noduleDistribution() {
        return Result.ok(statsService.noduleDistribution().stream()
                .map(dist -> new DistItemVO(dist.getDimension(), dist.getLabel(), dist.getCnt()))
                .toList());
    }

    @Operation(summary = "随访完成率/及时率月度趋势（完成率=已完成/应完成，及时率=done<=plan 占比，specs §4.2）")
    @GetMapping("/followup-rate")
    public Result<List<MonthlyRateVO>> followupRate(@RequestParam(defaultValue = "6") int months) {
        return Result.ok(statsService.followupRate(months).stream()
                .map(this::toMonthlyRate)
                .toList());
    }

    @Operation(summary = "医生工作量：最新快照日的建档数/完成任务数/预警处理数（按完成数降序）")
    @GetMapping("/doctor-workload")
    public Result<List<DoctorWorkloadVO>> doctorWorkload() {
        return Result.ok(statsService.doctorWorkload().stream()
                .map(w -> new DoctorWorkloadVO(w.getDoctorId(), w.getDoctorName(),
                        w.getArchiveCount(), w.getDoneCount(), w.getAlertCount()))
                .toList());
    }

    private MonthlyRateVO toMonthlyRate(StatsFollowupMonthly m) {
        return new MonthlyRateVO(m.getStatMonth(),
                StatsCalculator.completionRate(m.getDueCount(), m.getDoneCount()),
                StatsCalculator.timelyRate(m.getDueCount(), m.getTimelyCount()));
    }

    // ── 出参 VO（前端 ECharts 直接消费，specs/modules/statistics.md §4.3）──

    public record OverviewVO(Integer managingCount, Integer activePlanCount,
                             Integer overdueTaskCount, Integer alertCount, String updatedAt) {
    }

    public record DistItemVO(String dimension, String label, Integer cnt) {
    }

    public record MonthlyRateVO(String statMonth, double completionRate, double timelyRate) {
    }

    public record DoctorWorkloadVO(Long doctorId, String doctorName,
                                    Integer archiveCount, Integer doneCount, Integer alertCount) {
    }
}
