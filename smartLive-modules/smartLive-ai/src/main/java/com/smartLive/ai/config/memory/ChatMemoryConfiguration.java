package com.smartLive.ai.config.memory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * ChatMemory configuration.
 */
@Configuration
public class ChatMemoryConfiguration {

    /**
     * In-memory chat window memory.
     */
    @Primary
    @Bean("frameworkChatMemory")
    public ChatMemory frameworkChatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(100)
                .build();
    }
}