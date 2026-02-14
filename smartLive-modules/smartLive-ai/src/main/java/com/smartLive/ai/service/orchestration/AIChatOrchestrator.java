package com.smartLive.ai.service.orchestration;

import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.service.recognition.IntentRecognizer;
import com.smartLive.ai.strategy.factory.ChatHandlerFactory;
import com.smartLive.ai.strategy.handlers.ChatHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Orchestrates intent recognition and handler execution.
 */
@Slf4j
@Service
public class AIChatOrchestrator {

    @Autowired
    private IntentRecognizer intentRecognizer;

    @Autowired
    private ChatHandlerFactory chatHandlerFactory;

    public Flux<String> processMessage(AIChatRequest request) {
        chatHandlerFactory.getStrategies()
                .forEach(handler -> log.info("Loaded handler: {}", handler.getHandlerType()));
        log.info("Start processing message: {}", request.getMessage());

        try {
            String intent = intentRecognizer.recognizeIntent(request.getMessage());
            log.info("Recognized intent: {}", intent);

            ChatHandler handler = selectHandler(intent, request);
            Flux<String> response = handler.handle(request)
                    .onErrorResume(e -> {
                        log.error("Handler stream failed: {}", handler.getHandlerType(), e);
                        return Flux.just("抱歉，当前服务繁忙，请稍后重试。");
                    });

            log.info("Handler executed: {}", handler.getHandlerType());
            return response;
        } catch (Exception e) {
            log.error("Message processing failed", e);
            return Flux.just("Sorry, I'm not able to process your request.");
        }
    }

    private ChatHandler selectHandler(String intent, AIChatRequest request) {
        String handlerKey = intent + "Handler";
        ChatHandler handler = chatHandlerFactory.getExactStrategy(handlerKey);
        if (handler != null) {
            return handler;
        }

        for (ChatHandler item : chatHandlerFactory.getStrategies()) {
            if (item.canHandle(request.getMessage())) {
                return item;
            }
        }

        return chatHandlerFactory.getStrategy(handlerKey);
    }
}
