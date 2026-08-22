package com.yiliao.api.followup.dto;

import java.math.BigDecimal;

/**
 * 规则引擎输入特征（specs/modules/followup.md §4）。缺省字段不参与条件匹配。
 * noduleType: 1实性 2部分实性 3纯磨玻璃；stageGroup: CIS/I/II_III/EGFR_ADJ/IV/SCLC_LIMITED。
 */
public record RuleInputDTO(
        Integer noduleType,
        BigDecimal maxDiaMm,
        Boolean riskFactor,
        String stageGroup,
        Boolean genePositive,
        Boolean adjuvantTherapy
) {
}
