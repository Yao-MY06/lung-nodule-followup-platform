package com.yiliao.ai.llm;

import java.util.function.Consumer;

/**
 * LLM 访问抽象（specs/modules/ai.md §4.7）：实现层 Spring AI（OpenAI 兼容协议），
 * 换模型只改配置；单测用 Fake 实现，不触网。
 */
public interface LlmClient {

    /** 同步问答。 */
    String chat(String systemPrompt, String userPrompt);

    /** 流式问答：逐 delta 回调。 */
    void streamChat(String systemPrompt, String userPrompt, Consumer<String> onDelta);

    /** 要求返回严格 JSON 的问答（由实现层保证格式约束）。 */
    String chatForJson(String systemPrompt, String userPrompt);

    /** Fake 实现（测试/离线演示）。 */
    class Fake implements LlmClient {
        private final String jsonReply;
        private final String textReply;

        public Fake(String jsonReply, String textReply) {
            this.jsonReply = jsonReply;
            this.textReply = textReply;
        }

        @Override
        public String chat(String systemPrompt, String userPrompt) {
            return textReply;
        }

        @Override
        public void streamChat(String systemPrompt, String userPrompt, Consumer<String> onDelta) {
            for (char c : textReply.toCharArray()) {
                onDelta.accept(String.valueOf(c));
            }
        }

        @Override
        public String chatForJson(String systemPrompt, String userPrompt) {
            return jsonReply;
        }
    }
}
