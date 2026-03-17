package com.smartLive.ai.service.chat.support;

import java.util.List;

/**
 * 智能体路由决策类。
 */
public class AgentRoutingDecision {
    private final boolean collaborative;
    private final AgentType primaryAgent;
    private final List<AgentType> executionOrder;

    /**
     * 构造智能体路由决策。
     */
    public AgentRoutingDecision(boolean collaborative, AgentType primaryAgent, List<AgentType> executionOrder) {
        this.collaborative = collaborative;
        this.primaryAgent = primaryAgent;
        this.executionOrder = executionOrder;
    }

    /**
     * 判断是否为协作模式。
     */
    public boolean isCollaborative() {
        return collaborative;
    }

    /**
     * 获取主智能体。
     */
    public AgentType getPrimaryAgent() {
        return primaryAgent;
    }

    /**
     * 获取执行顺序。
     */
    public List<AgentType> getExecutionOrder() {
        return executionOrder;
    }
}
