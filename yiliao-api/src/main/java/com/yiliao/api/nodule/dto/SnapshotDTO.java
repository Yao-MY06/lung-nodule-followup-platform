package com.yiliao.api.nodule.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 结节快照（纵向趋势用）。
 */
public record SnapshotDTO(
        Long noduleId,
        LocalDate examDate,
        BigDecimal maxDiameterMm,
        BigDecimal solidDiameterMm,
        Integer meanDensityHu,
        Integer isNew
) {
}
