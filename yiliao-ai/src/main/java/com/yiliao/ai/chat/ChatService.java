package com.yiliao.ai.chat;

import com.yiliao.ai.rag.HybridRetriever;
import com.yiliao.ai.rag.KnowledgeBaseService;
import com.yiliao.ai.security.PatientChatGuard;
import com.yiliao.ai.tools.PatientTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

/**
 * A2/F4 智能问答（specs/modules/ai.md §4，flows/F4）：
 * RAG 上下文 + 工具调用（Spring AI 两步模式）+ SSE 流式 + 服务端强制引用与免责。
 * 知识库为空时明确告知（禁止行为 #2：不可用时不得自由发挥）。
 */
@Service
public class ChatService {

    public static final String DISCLAIMER = "\n\n—— 以上为 AI 辅助参考，不替代诊断；具体请遵主治医生意见。";

    private final ChatClient.Builder chatClientBuilder;
    private final KnowledgeBaseService knowledgeBase;
    private final PatientTools patientTools;
    private final PatientChatGuard patientChatGuard;

    public ChatService(ChatClient.Builder chatClientBuilder, KnowledgeBaseService knowledgeBase,
                       PatientTools patientTools, PatientChatGuard patientChatGuard) {
        this.chatClientBuilder = chatClientBuilder;
        this.knowledgeBase = knowledgeBase;
        this.patientTools = patientTools;
        this.patientChatGuard = patientChatGuard;
    }

    /** 引用溯源组装（specs/modules/ai.md §4.4：回答必须附出处）——纯函数供测试。 */
    static String buildCitation(List<HybridRetriever.DocChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\n\n参考指南：");
        for (HybridRetriever.DocChunk chunk : chunks) {
            sb.append("\n· ").append(chunk.docName()).append(" · ").append(chunk.section());
        }
        return sb.toString();
    }

    public SseEmitter chat(String message) {
        // 入口处完成角色校验 + 用户 id→档案 id 解析（PatientChatGuard，specs/modules/ai.md；
        // AGENTS.md 患者数据权限不变量），工具线程只消费不可变快照，不再读请求线程的 ThreadLocal。
        Long patientId = patientChatGuard.requirePatientArchive();
        PatientTools requestPatientTools = patientTools.forPatient(patientId);
        SseEmitter emitter = new SseEmitter(120_000L);
        List<HybridRetriever.DocChunk> contexts = knowledgeBase.isEmpty()
                ? List.of() : knowledgeBase.retrieve(message);

        String systemPrompt = buildSystemPrompt(contexts);
        chatClientBuilder.build()
                .prompt()
                .system(systemPrompt)
                .user(message)
                .tools(requestPatientTools)
                .stream()
                .content()
                .doOnNext(delta -> {
                    try {
                        emitter.send(SseEmitter.event().name("delta").data(delta));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .doOnComplete(() -> {
                    try {
                        emitter.send(SseEmitter.event().name("citation")
                                .data(buildCitation(contexts)));
                        emitter.send(SseEmitter.event().name("delta").data(DISCLAIMER));
                        emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                        emitter.complete();
                    } catch (IOException ignore) {
                        emitter.complete();
                    }
                })
                .doOnError(error -> {
                    try {
                        emitter.send(SseEmitter.event().name("delta")
                                .data("AI 服务暂时不可用，请稍后再试或联系您的医生。"));
                        emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                        emitter.complete();
                    } catch (IOException ignore) {
                        emitter.completeWithError(error);
                    }
                })
                .subscribe();
        return emitter;
    }

    static String buildSystemPrompt(List<HybridRetriever.DocChunk> contexts) {
        StringBuilder sb = new StringBuilder("""
                你是肺结节/肺癌随访助手，服务对象是患者与医生。规则：
                1. 涉及诊疗建议时，只依据提供的指南片段作答并注明出处；片段中没有的就说"指南片段未覆盖"，不得编造。
                2. 涉及患者个人数据（随访计划/结节趋势/报告结论/症状上报）时，调用提供的工具查询真实数据后再回答。
                3. 不给确定性诊断结论；语气平和，避免引起焦虑。
                """);
        if (contexts.isEmpty()) {
            sb.append("\n当前知识库不可用：如被问及指南知识，明确告知“知识库暂不可用，建议咨询医生”。\n");
        } else {
            sb.append("\n以下是检索到的指南片段：\n");
            for (HybridRetriever.DocChunk chunk : contexts) {
                sb.append("【").append(chunk.docName()).append("·").append(chunk.section()).append("】")
                        .append(chunk.text()).append("\n\n");
            }
        }
        return sb.toString();
    }
}
