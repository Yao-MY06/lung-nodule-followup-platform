package com.yiliao.ai.controller;

import com.yiliao.ai.chat.ChatService;
import com.yiliao.ai.error.SseBizExceptionHandler;
import com.yiliao.ai.extract.ReportExtractService;
import com.yiliao.ai.interpret.ReportInterpretService;
import com.yiliao.ai.rag.KnowledgeBaseService;
import com.yiliao.common.core.error.CommonErrorCode;
import com.yiliao.common.core.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SSE 端点业务异常的传输层契约测试（T4 遗留决策一，2026-09-05 定案）：
 * SSE Accept 下必须返回真实 HTTP 状态码 + text/event-stream 错误事件体
 * （event:error + data JSON），而非 500 空体；非 SSE Accept 下维持平台
 * "HTTP 200 + Result 业务码"惯例。守卫异常须先于任何 LLM 调用抛出。
 */
@ExtendWith(MockitoExtension.class)
class AiControllerSseErrorTest {

    @Mock
    private ReportExtractService extractService;
    @Mock
    private ReportInterpretService interpretService;
    @Mock
    private ChatService chatService;
    @Mock
    private KnowledgeBaseService knowledgeBase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AiController(extractService, interpretService, chatService, knowledgeBase))
                .setControllerAdvice(new SseBizExceptionHandler())
                .build();
    }

    @Test
    void chatBizErrorUnderSseAcceptReturnsRealStatusAndErrorEvent() throws Exception {
        when(chatService.chat("你好"))
                .thenThrow(new BizException(CommonErrorCode.FORBIDDEN.withMsg("AI 助手当前仅对患者端开放")));

        mockMvc.perform(post("/api/ai/chat")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"你好\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(allOf(
                        containsString("event:error"),
                        containsString("\"code\":403"),
                        containsString("AI 助手当前仅对患者端开放"))));

        verify(chatService).chat("你好");
    }

    @Test
    void chatBizErrorUnderNonSseAcceptKeepsPlatformResultConvention() throws Exception {
        when(chatService.chat("你好"))
                .thenThrow(new BizException(CommonErrorCode.FORBIDDEN.withMsg("AI 助手当前仅对患者端开放")));

        mockMvc.perform(post("/api/ai/chat")
                        .accept(MediaType.ALL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"你好\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("AI 助手当前仅对患者端开放"));
    }

    @Test
    void chatUnauthorizedUnderSseAcceptMapsToHttp401() throws Exception {
        when(chatService.chat("你好"))
                .thenThrow(new BizException(CommonErrorCode.UNAUTHORIZED));

        mockMvc.perform(post("/api/ai/chat")
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"你好\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(allOf(
                        containsString("event:error"),
                        containsString("\"code\":401"),
                        containsString(CommonErrorCode.UNAUTHORIZED.getMsg()))));
    }
}
