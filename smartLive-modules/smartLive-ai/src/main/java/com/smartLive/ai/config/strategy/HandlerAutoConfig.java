package com.smartLive.ai.config.strategy;
import com.smartLive.ai.strategy.handlers.ChatHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 基于 Spring 容器管理的策略分发模式
 */
@Configuration
public class HandlerAutoConfig {
    
    @Bean
    public Map<String, ChatHandler> chatHandlerMap(List<ChatHandler> handlers) {
        return handlers.stream()
                .collect(Collectors.toMap(
                    handler -> handler.getHandlerType() + "Handler",
                    Function.identity()
                ));
    }
}