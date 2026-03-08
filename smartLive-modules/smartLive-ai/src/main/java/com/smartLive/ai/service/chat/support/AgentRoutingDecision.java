package com.smartLive.ai.service.chat.support;

import java.util.List;

public class AgentRoutingDecision {
    private final boolean collaborative;
    private final AgentType primaryAgent;
    private final List<AgentType> executionOrder;

    public AgentRoutingDecision(boolean collaborative, AgentType primaryAgent, List<AgentType> executionOrder) {
        this.collaborative = collaborative;
        this.primaryAgent = primaryAgent;
        this.executionOrder = executionOrder;
    }

    public boolean isCollaborative() {
        return collaborative;
    }

    public AgentType getPrimaryAgent() {
        return primaryAgent;
    }

    public List<AgentType> getExecutionOrder() {
        return executionOrder;
    }
}
