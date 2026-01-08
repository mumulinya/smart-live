package com.smartLive.ai.strategy.handlers;

import com.smartLive.ai.entity.request.AIChatRequest;
import reactor.core.publisher.Flux;

/**
 * 使用策略模式创建bean
 */
public interface ChatHandler {
    
    /**
     * 处理器类型
     */
    String getHandlerType();
    
    /**
     * 判断是否能处理该消息
     */
    boolean canHandle(String message);
    
    /**
     * 处理消息
     */
    Flux<String> handle(AIChatRequest request);

    /**
     * 创建prompt工程
     */
    String buildPrompt(AIChatRequest request);

    /**
     * 处理器优先级（数值越小优先级越高）
     */
    default int getPriority() {
        return 5;
    }
}