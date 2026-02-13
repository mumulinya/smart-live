package com.smartLive.ai.strategy.factory;

import com.smartLive.ai.strategy.handlers.ChatHandler;
import com.smartLive.ai.strategy.handlers.DefaultHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory to retrieve the appropriate ChatHandler.
 */
@Component
public class ChatHandlerFactory {

    private final Map<String, ChatHandler> strategyMap = new HashMap<>();
    private final ChatHandler defaultStrategy;

    @Autowired
    public ChatHandlerFactory(List<ChatHandler> handlers, DefaultHandler defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
        for (ChatHandler handler : handlers) {
            strategyMap.put(handler.getHandlerType() + "Handler", handler);
        }
    }

    public ChatHandler getStrategy(String handlerKey) {
        return Optional.ofNullable(strategyMap.get(handlerKey)).orElse(defaultStrategy);
    }

    public ChatHandler getExactStrategy(String handlerKey) {
        return strategyMap.get(handlerKey);
    }

    public Collection<ChatHandler> getStrategies() {
        return strategyMap.values();
    }
}
