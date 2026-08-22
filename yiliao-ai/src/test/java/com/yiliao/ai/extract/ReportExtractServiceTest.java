package com.yiliao.ai.extract;

import com.yiliao.api.ai.dto.NoduleExtract;
import com.yiliao.ai.error.AiErrorCode;
import com.yiliao.ai.llm.LlmClient;
import com.yiliao.common.core.exception.BizException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportExtractServiceTest {

    private static final String GOOD_JSON = """
            {"location":"右肺上叶尖段","noduleType":"PURE_GG","maxDiameterMm":5.0,
             "solidDiameterMm":null,"count":1,"signs":["毛刺"],"impression":"磨玻璃结节，建议随访"}
            """;

    @Test
    void parsesStructuredResult() {
        ReportExtractService service = new ReportExtractService(new LlmClient.Fake(GOOD_JSON, ""));
        NoduleExtract extract = service.extract("右肺上叶磨玻璃结节 5mm");
        assertEquals("右肺上叶尖段", extract.location());
        assertEquals("PURE_GG", extract.noduleType());
        assertEquals(new BigDecimal("5.0"), extract.maxDiameterMm());
        assertNull(extract.solidDiameterMm());   // 原文没有 → null（缺失禁臆造）
        assertEquals(List.of("毛刺"), extract.signs());
    }

    @Test
    void promptIsSanitizedBeforeSend() {
        StringBuilder captured = new StringBuilder();
        LlmClient spy = new LlmClient.Fake(GOOD_JSON, "") {
            @Override
            public String chatForJson(String systemPrompt, String userPrompt) {
                captured.append(userPrompt);
                return super.chatForJson(systemPrompt, userPrompt);
            }
        };
        new ReportExtractService(spy).extract("患者电话13812345678，右肺结节4mm");
        assertTrue(captured.toString().contains("[电话]"));
        assertTrue(!captured.toString().contains("13812345678"));
    }

    @Test
    void badJsonRetriesOnceThenFails() {
        AtomicInteger calls = new AtomicInteger();
        LlmClient broken = new LlmClient.Fake("not-json{{", "") {
            @Override
            public String chatForJson(String systemPrompt, String userPrompt) {
                calls.incrementAndGet();
                return super.chatForJson(systemPrompt, userPrompt);
            }
        };
        BizException e = assertThrows(BizException.class,
                () -> new ReportExtractService(broken).extract("右肺结节"));
        assertEquals(AiErrorCode.EXTRACT_FAILED.getCode(), e.getCode());
        assertEquals(2, calls.get());   // 首次 + 纠错重试 1 次
    }

    @Test
    void recoversOnSecondAttempt() {
        AtomicInteger calls = new AtomicInteger();
        LlmClient flaky = new LlmClient.Fake(GOOD_JSON, "") {
            @Override
            public String chatForJson(String systemPrompt, String userPrompt) {
                return calls.incrementAndGet() == 1 ? "```json{bad" : GOOD_JSON;
            }
        };
        NoduleExtract extract = new ReportExtractService(flaky).extract("右肺结节");
        assertEquals("右肺上叶尖段", extract.location());
    }

    @Test
    void jsonCodeFenceStripped() throws Exception {
        NoduleExtract extract = ReportExtractService.parse("```json\n" + GOOD_JSON + "\n```");
        assertEquals("PURE_GG", extract.noduleType());
    }
}
