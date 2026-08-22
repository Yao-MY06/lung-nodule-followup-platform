package com.yiliao.ai.interpret;

import com.yiliao.ai.error.AiErrorCode;
import com.yiliao.ai.llm.LlmClient;
import com.yiliao.ai.llm.SensitiveDataFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * A1 报告通俗解读（specs/modules/ai.md §4.3，技术设计文档 §21）：
 * 四段式输出 + 免责语服务端强制追加；LLM 异常→SSE 内优雅降级（24001 文案，不抛错到前端）。
 */
@Service
public class ReportInterpretService {

    private static final Logger log = LoggerFactory.getLogger(ReportInterpretService.class);

    public static final String DISCLAIMER = "\n\n—— 以上内容为 AI 辅助参考，不构成诊断意见，请以主治医生意见为准。";

    static final String SYSTEM_PROMPT = """
            你是面向患者的胸外科随访助手，用通俗语言解读 CT 报告。固定输出四段：
            【这是什么】【风险怎么看】【下一步建议】【注意事项】。
            要求：只基于报告内容解释，不臆造；语气平和避免焦虑；不给出确定性诊断结论。
            """;

    private final LlmClient llmClient;

    public ReportInterpretService(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public SseEmitter interpret(String rawText) {
        SseEmitter emitter = new SseEmitter(60_000L);
        String sanitized = SensitiveDataFilter.sanitize(rawText);
        try {
            emitter.send(SseEmitter.event().name("delta").data(""));
            llmClient.streamChat(SYSTEM_PROMPT, sanitized, delta -> {
                try {
                    emitter.send(SseEmitter.event().name("delta").data(delta));
                } catch (IOException e) {
                    // 客户端断开，停止发送
                    throw new RuntimeException(e);
                }
            });
            // 免责语由服务端强制追加，不依赖模型自觉（specs/modules/ai.md 禁止行为 #2）
            emitter.send(SseEmitter.event().name("delta").data(DISCLAIMER));
            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
            emitter.complete();
        } catch (Exception e) {
            // 优雅降级：LLM 不可用 → 固定文案（specs/modules/ai.md §4.5 降级链）
            log.warn("解读降级：{}", e.getMessage());
            try {
                emitter.send(SseEmitter.event().name("delta")
                        .data(AiErrorCode.LLM_DEGRADED.getMsg()));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (IOException ignore) {
                emitter.completeWithError(e);
            }
        }
        return emitter;
    }
}
