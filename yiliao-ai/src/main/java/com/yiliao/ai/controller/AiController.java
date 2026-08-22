package com.yiliao.ai.controller;

import com.yiliao.ai.chat.ChatService;
import com.yiliao.ai.error.AiErrorCode;
import com.yiliao.ai.extract.ReportExtractService;
import com.yiliao.ai.interpret.ReportInterpretService;
import com.yiliao.ai.rag.KnowledgeBaseService;
import com.yiliao.api.ai.dto.NoduleExtract;
import com.yiliao.common.core.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 接口（specs/modules/ai.md §3）。
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final ReportExtractService extractService;
    private final ReportInterpretService interpretService;
    private final ChatService chatService;
    private final KnowledgeBaseService knowledgeBase;

    public AiController(ReportExtractService extractService, ReportInterpretService interpretService,
                        ChatService chatService, KnowledgeBaseService knowledgeBase) {
        this.extractService = extractService;
        this.interpretService = interpretService;
        this.chatService = chatService;
        this.knowledgeBase = knowledgeBase;
    }

    @Operation(summary = "CT 报告结构化抽取（医生确认前的草稿）")
    @PostMapping("/report/extract")
    public Result<NoduleExtract> extract(@jakarta.validation.Valid @RequestBody ExtractRequest request) {
        return Result.ok(extractService.extract(request.rawText()));
    }

    @Operation(summary = "报告 AI 通俗解读（SSE 流式，末尾强制免责）")
    @PostMapping(value = "/report/interpret", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter interpret(@jakarta.validation.Valid @RequestBody ExtractRequest request) {
        return interpretService.interpret(request.rawText());
    }

    @Operation(summary = "智能问答/助手（RAG + Tool Calling，SSE）")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@jakarta.validation.Valid @RequestBody ChatRequest request) {
        return chatService.chat(request.message());
    }

    @Operation(summary = "知识库状态")
    @GetMapping("/knowledge/status")
    public Result<Integer> knowledgeStatus() {
        return Result.ok(knowledgeBase.isEmpty() ? 0 : 1);
    }

    public record ExtractRequest(@NotBlank String rawText) {
    }

    public record ChatRequest(@NotBlank String message) {
    }
}
