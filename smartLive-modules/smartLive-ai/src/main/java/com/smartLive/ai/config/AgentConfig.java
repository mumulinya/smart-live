package com.smartLive.ai.config;

import com.smartLive.ai.tools.CommentTools;
import com.smartLive.ai.tools.ProductTools;
import com.smartLive.ai.tools.ShopTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
public class AgentConfig {

    @Bean("shopAgent")
    public ChatClient shopAgent(
            @Qualifier("openAiChatModel") ChatModel chatModel,
            ChatMemory chatMemory,
            ShopTools shopTools,
            ProductTools productTools,
            CommentTools commentTools
    ) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一个智能助手，帮助用户查询店铺信息。
                        你可以使用以下工具：
                        1. searchShopsByCategory - 搜索店铺
                        2. getShopDetails - 查询店铺详情
                        3. searchGroupBuyingDeals - 搜索团购套餐
                        4. searchTantanNotes - 搜索探店笔记
                        """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(shopTools, productTools, commentTools)
                .build();
    }
}
