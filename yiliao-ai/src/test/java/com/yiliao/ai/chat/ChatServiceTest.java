package com.yiliao.ai.chat;

import com.yiliao.ai.rag.KnowledgeBaseService;
import com.yiliao.ai.security.PatientChatGuard;
import com.yiliao.ai.tools.PatientTools;
import com.yiliao.common.core.error.CommonErrorCode;
import com.yiliao.common.core.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 智能问答入口守卫单测：守卫先于任何流式调用执行；
 * 守卫拒绝时请求不得触达 LLM（specs/modules/ai.md；AGENTS.md 患者数据权限不变量）。
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.StreamResponseSpec streamResponseSpec;
    @Mock
    private KnowledgeBaseService knowledgeBase;
    @Mock
    private PatientTools patientTools;
    @Mock
    private PatientChatGuard patientChatGuard;

    @Test
    void chatRunsGuardFirstAndBindsPatientToolView() {
        when(patientChatGuard.requirePatientArchive()).thenReturn(1009L);
        when(patientTools.forPatient(1009L)).thenReturn(patientTools);
        when(knowledgeBase.isEmpty()).thenReturn(true);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.tools(any(PatientTools.class))).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.content()).thenReturn(Flux.empty());

        ChatService service = new ChatService(chatClientBuilder, knowledgeBase, patientTools, patientChatGuard);
        SseEmitter emitter = service.chat("你好");

        assertNotNull(emitter);
        // 守卫先于流式调用执行（否则 builder.build() 的 stub 不会被消费到校验之前）
        verify(patientChatGuard).requirePatientArchive();
        // 工具视图已绑定进请求 spec
        verify(requestSpec).tools(any(PatientTools.class));
        // 知识库为空时不检索（禁止行为：不可用时不得自由发挥）
        verify(knowledgeBase, never()).retrieve("你好");
    }

    @Test
    void chatRejectsBeforeTouchingLlmWhenGuardDenies() {
        when(patientChatGuard.requirePatientArchive())
                .thenThrow(new BizException(CommonErrorCode.FORBIDDEN.withMsg("AI 助手当前仅对患者端开放")));

        ChatService service = new ChatService(chatClientBuilder, knowledgeBase, patientTools, patientChatGuard);

        assertThrows(BizException.class, () -> service.chat("你好"));
        verify(chatClientBuilder, never()).build();
    }
}
