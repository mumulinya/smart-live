package com.smartLive.ai.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIClient {
    private final ChatClient restaurantChatClient;
    public Flux<String> generate(String prompt, String chatId) {
        log.info("🚀 第二次调用大模型API");
        
        // 8.1 调用AI API（OpenAI/文心一言等）流式输出
        Flux<String> response = callAIModel(prompt, chatId);
//        log.info("✅ AI调用成功，返回数据为: {}", response);
        return response;
    }
    public String firstGenerate(String prompt) {
        log.info("🚀 首次调用大模型API");
        log.info("🎯 模型输入: {}", prompt);
        // 8.1 调用AI API（OpenAI/文心一言等）流式输出
        String response = firstCallAIModel(prompt);
        log.info("✅ AI调用成功，返回数据为: {}", response);
        return response;
    }
    private Flux<String> callAIModel(String prompt, String chatId) {
        // 实际调用AI模型的HTTP请求
        // 例如：OpenAI API、本地模型等
        Flux<String> content = restaurantChatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                .stream()
                .content();
        return content;
    }

    private String firstCallAIModel(String prompt) {
        // 实际调用AI模型的HTTP请求
        // 例如：OpenAI API、本地模型等
        String content = restaurantChatClient.prompt()
                .user(prompt)
                .call()

                .content();
        return content;
    }
}