package com.yiliao.api.ai.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * CT 报告 AI 结构化抽取结果（specs/modules/ai.md §3）。
 * 仅抽取原文存在的信息，缺失字段为 null，禁止臆造；AI 只起草、医生确认后入库。
 */
public record NoduleExtract(
        String location,
        String noduleType,          // SOLID / PART_SOLID / PURE_GG
        BigDecimal maxDiameterMm,
        BigDecimal solidDiameterMm,
        Integer count,
        List<String> signs,         // 恶性征象
        String impression           // 影像结论
) {
}
