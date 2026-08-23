package com.yiliao.statistics.service;

import com.yiliao.statistics.entity.StatsDoctorWorkload;
import com.yiliao.statistics.entity.StatsFollowupMonthly;
import com.yiliao.statistics.entity.StatsOverviewDaily;
import com.yiliao.statistics.mapper.StatsDoctorWorkloadMapper;
import com.yiliao.statistics.mapper.StatsFollowupMonthlyMapper;
import com.yiliao.statistics.mapper.StatsNoduleDistMapper;
import com.yiliao.statistics.mapper.StatsOverviewDailyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 快照聚合/查询单测：refreshAll 先删后插、空表返回空集合/零值对象（specs/modules/statistics.md §5）。
 */
@ExtendWith(MockitoExtension.class)
class StatsAggregateServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 23);
    private static final String MONTH = "2026-08";

    @Mock
    private StatsOverviewDailyMapper overviewMapper;
    @Mock
    private StatsFollowupMonthlyMapper monthlyMapper;
    @Mock
    private StatsNoduleDistMapper distMapper;
    @Mock
    private StatsDoctorWorkloadMapper workloadMapper;
    @Mock
    private StatsCollector collector;

    private StatsAggregateService service;

    @BeforeEach
    void setUp() {
        service = new StatsAggregateService(overviewMapper, monthlyMapper, distMapper,
                workloadMapper, collector);
    }

    @Test
    void refreshAllDeletesOldSnapshotBeforeInsert() {
        when(collector.collectOverview(DATE)).thenReturn(new StatsCollector.OverviewData(356, 312, 23, 18));
        when(collector.collectMonthly(MONTH)).thenReturn(new StatsCollector.MonthlyData(480, 438, 401));
        when(collector.collectNoduleDist()).thenReturn(List.of(
                new StatsCollector.NoduleDistItem("nodule_type", "实性", 128)));
        when(collector.collectDoctorWorkload(DATE)).thenReturn(List.of(
                new StatsCollector.DoctorWorkloadItem(1L, "陈国栋", 4, 9, 2)));

        service.refreshAll(DATE);

        // 四张快照表均先物理删除旧快照、再插入新快照
        InOrder inOrder = inOrder(overviewMapper, monthlyMapper, distMapper, workloadMapper);
        inOrder.verify(overviewMapper).physicalDeleteByDate(DATE);
        inOrder.verify(overviewMapper).insert(any(StatsOverviewDaily.class));
        inOrder.verify(monthlyMapper).physicalDeleteByMonth(MONTH);
        inOrder.verify(monthlyMapper).insert(any(StatsFollowupMonthly.class));
        inOrder.verify(distMapper).physicalDeleteAll();
        inOrder.verify(distMapper).insert(anyCollection());
        inOrder.verify(workloadMapper).physicalDeleteByDate(DATE);
        inOrder.verify(workloadMapper).insert(anyCollection());

        // 总览快照值正确透传入库
        ArgumentCaptor<StatsOverviewDaily> captor = ArgumentCaptor.forClass(StatsOverviewDaily.class);
        verify(overviewMapper).insert(captor.capture());
        assertEquals(DATE, captor.getValue().getStatDate());
        assertEquals(356, captor.getValue().getManagingCount());
        assertEquals(18, captor.getValue().getAlertCount());
    }

    @Test
    void refreshAllSkipsEmptyBatchInsert() {
        when(collector.collectOverview(DATE)).thenReturn(new StatsCollector.OverviewData(0, 0, 0, 0));
        when(collector.collectMonthly(MONTH)).thenReturn(new StatsCollector.MonthlyData(0, 0, 0));
        when(collector.collectNoduleDist()).thenReturn(List.of());
        when(collector.collectDoctorWorkload(DATE)).thenReturn(List.of());

        service.refreshAll(DATE);

        // 空集合不触发批量 insert（多值 SQL 空集合非法）；单条快照仍写入零值行
        verify(distMapper, never()).insert(anyCollection());
        verify(workloadMapper, never()).insert(anyCollection());
        verify(overviewMapper).insert(any(StatsOverviewDaily.class));
    }

    @Test
    void overviewReturnsZeroValueObjectOnEmptyTable() {
        when(overviewMapper.selectOne(any())).thenReturn(null);

        StatsOverviewDaily daily = service.overview();

        assertNotNull(daily);
        assertEquals(0, daily.getManagingCount());
        assertEquals(0, daily.getActivePlanCount());
        assertEquals(0, daily.getOverdueTaskCount());
        assertEquals(0, daily.getAlertCount());
    }

    @Test
    void listQueriesReturnEmptyCollectionsOnEmptyTables() {
        when(distMapper.selectList(any())).thenReturn(List.of());
        when(monthlyMapper.selectList(any())).thenReturn(List.of());
        when(workloadMapper.selectOne(any())).thenReturn(null);

        assertTrue(service.noduleDistribution().isEmpty());
        assertTrue(service.followupRate(6).isEmpty());
        assertTrue(service.doctorWorkload().isEmpty());
    }

    @Test
    void followupRateReturnsAscendingMonths() {
        when(monthlyMapper.selectList(any())).thenReturn(List.of(monthRow("2026-08"), monthRow("2026-07")));

        List<StatsFollowupMonthly> result = service.followupRate(6);

        assertEquals(2, result.size());
        assertEquals("2026-07", result.get(0).getStatMonth());
        assertEquals("2026-08", result.get(1).getStatMonth());
    }

    @Test
    void doctorWorkloadReturnsLatestDateRows() {
        StatsDoctorWorkload latest = new StatsDoctorWorkload();
        latest.setStatDate(DATE);
        when(workloadMapper.selectOne(any())).thenReturn(latest);
        when(workloadMapper.selectList(any())).thenReturn(List.of(new StatsDoctorWorkload()));

        assertEquals(1, service.doctorWorkload().size());
    }

    private StatsFollowupMonthly monthRow(String month) {
        StatsFollowupMonthly row = new StatsFollowupMonthly();
        row.setStatMonth(month);
        row.setDueCount(480);
        row.setDoneCount(438);
        row.setTimelyCount(401);
        return row;
    }
}
