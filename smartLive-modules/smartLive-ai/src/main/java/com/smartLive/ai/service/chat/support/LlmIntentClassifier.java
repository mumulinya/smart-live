package com.smartLive.ai.service.chat.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 基于 LLM 的意图分类器。
 */
@Slf4j
@Component
public class LlmIntentClassifier {

    private final ChatClient intentRouterChatClient;

    /**
     * 构造 LLM 意图分类器。
     */
    public LlmIntentClassifier(@Qualifier("intentRouterChatClient") ChatClient intentRouterChatClient) {
        this.intentRouterChatClient = intentRouterChatClient;
    }

    /**
     * 分类智能体类型。
     */
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
                log.info("Intent classification attempt {}", attempt);
                String result = intentRouterChatClient.prompt()
                        .user(userMessage)
                        .call()
                        .content();

                if (result == null) {
                    return AgentType.GENERAL;
                }

                String normalized = result.trim().toUpperCase();
                log.info("Intent classifier result: {}", normalized);

                if (normalized.contains("SHOP")) {
                    return AgentType.SHOP;
                }
                if (normalized.contains("PRODUCT")) {
                    return AgentType.PRODUCT;
                }
                if (normalized.contains("REVIEW")) {
                    return AgentType.REVIEW;
                }
                return AgentType.GENERAL;
            }
            catch (Exception e) {
                lastException = e;
                log.warn("Intent classification failed, retrying... ({}/{})", attempt, maxRetries, e);
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(500);
                    }
                    catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.warn("Intent classification failed after {} attempts, fallback to keyword rules", maxRetries, lastException);
        String msg = userMessage.toLowerCase();

        if (msg.contains("product")
                || msg.contains("\u5546\u54c1")
                || msg.contains("\u56e2\u8d2d")
                || msg.contains("\u5957\u9910")
                || msg.contains("\u4f18\u60e0\u5238")
                || msg.contains("\u5238")
                || msg.contains("\u4e0b\u5355")
                || msg.contains("\u8d2d\u4e70")
                || msg.contains("\u5e93\u5b58")
                || msg.contains("\u4ef7\u683c")) {
            return AgentType.PRODUCT;
        }
        if (msg.contains("review")
                || msg.contains("\u8bc4\u4ef7")
                || msg.contains("\u8bc4\u8bba")
                || msg.contains("\u53e3\u7891")
                || msg.contains("\u8bc4\u5206")
                || msg.contains("\u597d\u8bc4")
                || msg.contains("\u5dee\u8bc4")) {
            return AgentType.REVIEW;
        }
        if (msg.contains("shop")
                || msg.contains("\u5e97\u94fa")
                || msg.contains("\u5546\u5bb6")
                || msg.contains("\u9910\u5385")
                || msg.contains("\u597d\u5403")
                || msg.contains("\u63a8\u8350")
                || msg.contains("\u63a2\u5e97")
                || msg.contains("\u79cd\u8349")
                || msg.contains("\u7b14\u8bb0")
                || msg.contains("\u535a\u5ba2")) {
            return AgentType.SHOP;
        }

        return AgentType.GENERAL;
    }
}
