package com.yiliao.nodule.compare;

import com.yiliao.nodule.entity.NoduleSnapshot;
import com.yiliao.nodule.mapper.NoduleSnapshotMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompareServiceTest {

    @Mock
    private NoduleSnapshotMapper snapshotMapper;

    private NoduleSnapshot snapshot(long id, LocalDate date, String maxDia, String solidDia) {
        NoduleSnapshot snapshot = new NoduleSnapshot();
        snapshot.setId(id);
        snapshot.setExamDate(date);
        snapshot.setMaxDiameterMm(maxDia == null ? null : new BigDecimal(maxDia));
        snapshot.setSolidDiameterMm(solidDia == null ? null : new BigDecimal(solidDia));
        return snapshot;
    }

    @Test
    void growthOfTwoMmOrMoreIsProgress() {
        // 5.0 → 8.0：Δ=3.0 ≥2mm 判进展（flows/F3 验证清单 #1）
        when(snapshotMapper.selectList(any())).thenReturn(List.of(
                snapshot(2, LocalDate.of(2026, 9, 10), "8.0", "5.0"),
                snapshot(1, LocalDate.of(2026, 3, 10), "5.0", "4.5")));
        CompareService compareService = new CompareService(snapshotMapper);

        CompareService.CompareVO vo = compareService.compare(101L);
        assertTrue(vo.progress());
        assertEquals(new BigDecimal("3.0"), vo.deltaMaxMm());
        assertTrue(vo.advice().contains("升级"));
    }

    @Test
    void solidComponentGrowthTriggersProgress() {
        // 实性成分 4.0 → 6.5（Δ2.5），最大径不变（F3 验证清单 #3）
        when(snapshotMapper.selectList(any())).thenReturn(List.of(
                snapshot(2, LocalDate.of(2026, 9, 10), "10.0", "6.5"),
                snapshot(1, LocalDate.of(2026, 3, 10), "10.0", "4.0")));
        CompareService compareService = new CompareService(snapshotMapper);

        assertTrue(compareService.compare(101L).progress());
    }

    @Test
    void smallChangeIsStable() {
        // 6.0 → 6.5：Δ0.5 <2mm 不触发（F3 验证清单 #2）
        when(snapshotMapper.selectList(any())).thenReturn(List.of(
                snapshot(2, LocalDate.of(2026, 9, 10), "6.5", null),
                snapshot(1, LocalDate.of(2026, 3, 10), "6.0", null)));
        CompareService compareService = new CompareService(snapshotMapper);

        CompareService.CompareVO vo = compareService.compare(101L);
        assertFalse(vo.progress());
        assertTrue(vo.advice().contains("稳定"));
    }

    @Test
    void exactlyTwoMmIsProgress() {
        // 恰好 2.0：阈值含等号（≥2mm）
        when(snapshotMapper.selectList(any())).thenReturn(List.of(
                snapshot(2, LocalDate.of(2026, 9, 10), "7.0", null),
                snapshot(1, LocalDate.of(2026, 3, 10), "5.0", null)));
        CompareService compareService = new CompareService(snapshotMapper);

        assertTrue(compareService.compare(101L).progress());
    }

    @Test
    void singleSnapshotIsBaselineOnly() {
        when(snapshotMapper.selectList(any())).thenReturn(List.of(
                snapshot(1, LocalDate.of(2026, 3, 10), "5.0", "4.0")));
        CompareService compareService = new CompareService(snapshotMapper);

        CompareService.CompareVO vo = compareService.compare(101L);
        assertFalse(vo.progress());
        assertNull(vo.deltaMaxMm());
        assertTrue(vo.advice().contains("基线"));
    }

    @Test
    void missingDimensionDoesNotBreakComparison() {
        // 实性成分缺失：按最大径判断
        when(snapshotMapper.selectList(any())).thenReturn(List.of(
                snapshot(2, LocalDate.of(2026, 9, 10), "9.0", null),
                snapshot(1, LocalDate.of(2026, 3, 10), "5.0", null)));
        CompareService compareService = new CompareService(snapshotMapper);

        CompareService.CompareVO vo = compareService.compare(101L);
        assertNull(vo.deltaSolidMm());
        assertTrue(vo.progress());
    }
}
