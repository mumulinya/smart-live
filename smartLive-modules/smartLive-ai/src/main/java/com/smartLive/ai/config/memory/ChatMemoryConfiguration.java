package com.smartLive.ai.config.memory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 聊天记忆配置类。
 */
@Configuration
public class ChatMemoryConfiguration {

    // 当前先控制为 30 条，后续需要可再调回 100。
    public static final int MAX_MESSAGES = 30;

    /**
     * 获取聊天记忆。
     */
    @Primary
    @Bean("frameworkChatMemory")
    public ChatMemory frameworkChatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(MAX_MESSAGES)
                .build();
    }
}
