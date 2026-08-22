package com.yiliao.ai.extract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiliao.ai.error.AiErrorCode;
import com.yiliao.ai.llm.LlmClient;
import com.yiliao.ai.llm.SensitiveDataFilter;
import com.yiliao.api.ai.dto.NoduleExtract;
import com.yiliao.common.core.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * A3 报告结构化抽取（specs/modules/ai.md §4.2，技术设计文档 §18）：
 * 脱敏 → Prompt 约束（仅抽取原文、缺失 null 禁臆造、严格 JSON）→ 解析；
 * 坏 JSON 重试 1 次（附纠错提示），仍失败 24002 不阻塞人工录入。
 */
@Service
public class ReportExtractService {

    private static final Logger log = LoggerFactory.getLogger(ReportExtractService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static final String SYSTEM_PROMPT = """
            你是放射科结构化助手。从 CT 报告原文抽取肺结节信息，输出严格 JSON：
            {"location":"位置","noduleType":"SOLID|PART_SOLID|PURE_GG","maxDiameterMm":数值,
             "solidDiameterMm":数值或null,"count":数值,"signs":["恶性征象"],"impression":"影像结论"}
            规则：只抽取原文明确存在的信息；原文没有的字段返回 null，禁止臆造或推算；数值不带单位。
            """;

    static final String RETRY_HINT = "\n上次输出不是合法 JSON，请重新只输出符合 schema 的 JSON。";

    private final LlmClient llmClient;

    public ReportExtractService(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public NoduleExtract extract(String rawText) {
        String sanitized = SensitiveDataFilter.sanitize(rawText);
        if (SensitiveDataFilter.containsSensitive(sanitized)) {
            log.error("脱敏后仍检出敏感残留，终止出网（不应发生）");
            throw new BizException(AiErrorCode.EXTRACT_FAILED);
        }
        try {
            return parse(llmClient.chatForJson(SYSTEM_PROMPT, sanitized));
        } catch (BizException e) {
            throw e;
        } catch (Exception first) {
            log.warn("抽取首次解析失败，重试一次：{}", first.getMessage());
            try {
                return parse(llmClient.chatForJson(SYSTEM_PROMPT, sanitized + RETRY_HINT));
            } catch (Exception second) {
                log.warn("抽取重试仍失败", second);
                throw new BizException(AiErrorCode.EXTRACT_FAILED);
            }
        }
    }

    static NoduleExtract parse(String json) throws Exception {
        // 容错：剥掉可能的 ```json 代码块包裹
        String cleaned = json == null ? "" : json.trim()
                .replaceAll("^```(json)?", "").replaceAll("```$", "").trim();
        return MAPPER.readValue(cleaned, NoduleExtract.class);
    }
}
