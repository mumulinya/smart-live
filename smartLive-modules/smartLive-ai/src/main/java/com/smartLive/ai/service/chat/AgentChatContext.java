package com.smartLive.ai.service.chat;

import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.service.chat.strategy.AutonomousAgentStrategy;
import com.smartLive.ai.service.chat.strategy.DirectRoutingStrategy;
import com.smartLive.ai.service.chat.strategy.FrameworkRoutingStrategy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Agent 聊天策略上下文（工厂/路由分发器）。
 * 负责根据请求参数和配置属性决定实际采用哪一种聊天策略。
 */
@Component
public class AgentChatContext {

    private final FrameworkRoutingStrategy frameworkRoutingStrategy;
    private final DirectRoutingStrategy directRoutingStrategy;
    private final AutonomousAgentStrategy autonomousAgentStrategy;
    private final Environment environment;

    public AgentChatContext(
            FrameworkRoutingStrategy frameworkRoutingStrategy,
            DirectRoutingStrategy directRoutingStrategy,
            AutonomousAgentStrategy autonomousAgentStrategy,
            Environment environment
    ) {
        this.frameworkRoutingStrategy = frameworkRoutingStrategy;
        this.directRoutingStrategy = directRoutingStrategy;
        this.autonomousAgentStrategy = autonomousAgentStrategy;
        this.environment = environment;
    }

    /**
     * 生成聊天响应。
     */
    public Flux<String> generateResponse(AIChatRequest chatRequest, Boolean autonomous) {
        boolean useAuto = (autonomous != null) ? autonomous : false;
        if (useAuto) {
            return autonomousAgentStrategy.streamChat(chatRequest);
        }

        boolean frameworkEnabled = environment.getProperty("smartlive.ai.framework.enabled", Boolean.class, true);
        if (frameworkEnabled) {
            return frameworkRoutingStrategy.streamChat(chatRequest);
        } else {
            return directRoutingStrategy.streamChat(chatRequest);
        }
    }
}
