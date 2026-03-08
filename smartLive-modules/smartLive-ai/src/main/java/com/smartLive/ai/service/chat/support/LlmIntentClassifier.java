package com.smartLive.ai.service.chat.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 智能意图分类器。
 * 专为项目第二套（原生自建体系）提供基于大模型的智能路由决策。
 */
@Slf4j
@Component
public class LlmIntentClassifier {

    private final ChatClient intentRouterChatClient;

    public LlmIntentClassifier(@Qualifier("intentRouterChatClient") ChatClient intentRouterChatClient) {
        this.intentRouterChatClient = intentRouterChatClient;
    }

    public AgentType classify(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return AgentType.GENERAL;
        }

        int maxRetries = 3;
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                attempt++;
                log.info("智能路由大模型决策请求第 {} 次尝试", attempt);
                // 1. 调用专用的智能路由 ChatClient 进行判断
                String result = intentRouterChatClient.prompt()
                        .user(userMessage)
                        .call()
                        .content();

                if (result == null) {
                    return AgentType.GENERAL;
                }

                String msgResult = result.trim().toUpperCase();
                log.info("自建 Agent 智能路由大模型决策结果: {}", msgResult);

                // 2. 解析大模型吐出的意图标签
                if (msgResult.contains("SHOP")) {
                    return AgentType.SHOP;
                } else if (msgResult.contains("PRODUCT")) {
                    return AgentType.PRODUCT;
                } else if (msgResult.contains("REVIEW")) {
                    return AgentType.REVIEW;
                }

                return AgentType.GENERAL;
            } catch (Exception e) {
                lastException = e;
                log.warn("自建 Agent 智能路由大模型调用异常 (尝试 {}/{})", attempt, maxRetries, e);
                // 等待一小段时间后再重试 (例如 500ms)
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.warn("自建 Agent 智能路由大模型在 {} 次重试后仍然失败，降级为正则表达式匹配", maxRetries, lastException);
        // 降级保护：如果大模型超时或报错，使用正则兜底
        String msg = userMessage.toLowerCase();
        
        if (msg.contains("shop") || msg.contains("店铺") || msg.contains("商家")) {
            return AgentType.SHOP;
        }
        if (msg.contains("product") || msg.contains("商品") || msg.contains("价格") || msg.contains("优惠") || msg.contains("买")) {
            return AgentType.PRODUCT;
        }
        if (msg.contains("review") || msg.contains("评价") || msg.contains("质量") || msg.contains("差评") || msg.contains("好评")) {
            return AgentType.REVIEW;
        }

        return AgentType.GENERAL;
    }
}
