package com.smartLive.ai.service.orchestration;

import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.strategy.handlers.ChatHandler;
import com.smartLive.ai.service.recognition.IntentRecognizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 总指挥，负责协调意图识别和处理器执行
 */
@Slf4j
@Service
public class AIChatOrchestrator {
    
    @Autowired
    private IntentRecognizer intentRecognizer;
    
    @Autowired
    private Map<String, ChatHandler> chatHandlers; // Spring会自动注入所有实现
    
    public Flux<String> processMessage(AIChatRequest request) {
        chatHandlers.forEach((key, handler) -> log.info("🔧 已加载处理器: {} ({})", key, handler.getHandlerType()));
        log.info("🔍 开始处理消息: {}", request.getMessage());
        
        try {
            // 1. 识别意图
            String intent = intentRecognizer.recognizeIntent(request.getMessage());
            log.info("🎯 识别意图: {}", intent);
            
            // 2. 选择处理器
            ChatHandler handler = selectHandler(intent, request);
            if (handler == null) {
                log.info("⚠️ 未找到合适的处理器，使用默认处理器");
                handler = chatHandlers.get("defaultHandler");
            }
            
            // 3. 执行处理
            Flux<String> response = handler.handle(request);
            
            log.info("🎉 处理器执行完成: {}", handler.getHandlerType());
            return response;
            
        } catch (Exception e) {
            log.error("❌ 消息处理失败", e);
            return Flux.just("Sorry, I'm not able to process your request.");
        }
    }
    
    private ChatHandler selectHandler(String intent, AIChatRequest request) {
        String handlerBeanName = intent + "Handler";
        ChatHandler handler = chatHandlers.get(handlerBeanName);
        log.info("🔍 选择处理器: {}", handler);
        if (handler != null) {
            return handler;
        }
        
        // 遍历所有处理器，找到能处理的
        for (ChatHandler h : chatHandlers.values()) {
            if (h.canHandle(request.getMessage())) {
                return h;
            }
        }
        
        return chatHandlers.get("defaultHandler");
    }
}