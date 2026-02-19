package com.smartLive.ai.config;

import com.smartLive.ai.tools.CommentTools;
import com.smartLive.ai.tools.ProductTools;
import com.smartLive.ai.tools.ShopTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

@Configuration
public class CommonConfiguration {

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(100)
                .build();
    }

    @Bean
    @Description("餐厅聊天机器人")
    public ChatClient restaurantChatClient(@Qualifier("openAiChatModel") ChatModel model, ChatMemory chatMemory, ShopTools shopTools, ProductTools productTools, CommentTools commentTools) {
        return ChatClient
                .builder(model)
//                .defaultSystem(SystemConstants.RESTAURANT_SYSTEM_PROMPT)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultTools(shopTools, productTools, commentTools)
                .build();
    }
}
