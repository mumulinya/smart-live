package com.smartLive.ai.service.chat.support;

import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;

@Component
public class AgentRouter {

    private final LlmIntentClassifier intentClassifier;

    public AgentRouter(LlmIntentClassifier intentClassifier) {
        this.intentClassifier = intentClassifier;
    }

    public AgentRoutingDecision routeDecision(String userMessage) {
        if (userMessage == null) {
            return new AgentRoutingDecision(false, AgentType.GENERAL, List.of(AgentType.GENERAL));
        }
        AgentType primary = intentClassifier.classify(userMessage);

        boolean isCollaborative = userMessage.contains("对比") || userMessage.contains("综合") || userMessage.contains("比较");
        
        if (isCollaborative) {
            return new AgentRoutingDecision(true, primary, Arrays.asList(AgentType.SHOP, AgentType.PRODUCT, AgentType.REVIEW));
        }
        
        return new AgentRoutingDecision(false, primary, List.of(primary));
    }
}
