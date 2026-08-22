package com.yiliao.ai.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Spring AI 实现（OpenAI 兼容协议，模型可插拔 specs/modules/ai.md §4.7）。
 * 依赖 spring-ai-starter-model-openai 自动装配的 ChatModel。
 */
@Component
public class SpringAiLlmClient implements LlmClient {

    private final ChatClient chatClient;

    public SpringAiLlmClient(org.springframework.ai.chat.model.ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }

    @Override
    public void streamChat(String systemPrompt, String userPrompt, java.util.function.Consumer<String> onDelta) {
        chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content()
                .doOnNext(onDelta)
                .blockLast();
    }

    @Override
    public String chatForJson(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt + "\n\n只输出 JSON，不要输出任何其他文字或代码块标记。");
    }
}
