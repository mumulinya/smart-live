package com.smartLive.ai.service.chat.strategy;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.service.chat.AgentChatStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.util.List;

@Slf4j
@Service
public class FrameworkRoutingStrategy implements AgentChatStrategy {

    private static final String ROUTER_NAME = "smartlive_auto_router";
    private static final String GENERAL_AGENT_NAME = "general_agent";

    private final Environment environment;
    private final ChatModel frameworkChatModel;
    private final Agent shopAgent;
    private final Agent productAgent;
    private final Agent reviewAgent;
    private final Agent generalAgent;
    private final SpringAIJacksonStateSerializer stateSerializer;

    private volatile LlmRoutingAgent routingAgent;

    public FrameworkRoutingStrategy(
            Environment environment,
            @Qualifier("frameworkChatModel") ChatModel frameworkChatModel,
            @Qualifier("shopAgent") Agent shopAgent,
            @Qualifier("productAgent") Agent productAgent,
            @Qualifier("reviewAgent") Agent reviewAgent,
            @Qualifier("generalAgent") Agent generalAgent
    ) {
        this.environment = environment;
        this.frameworkChatModel = frameworkChatModel;
        this.shopAgent = shopAgent;
        this.productAgent = productAgent;
        this.reviewAgent = reviewAgent;
        this.generalAgent = generalAgent;
        this.stateSerializer = new SpringAIJacksonStateSerializer(OverAllState::new);
    }

    @Override
    public Flux<String> streamChat(AIChatRequest chatRequest) {
        LlmRoutingAgent agent = getOrCreateRoutingAgent();
        if (agent == null) {
            return Flux.error(new IllegalStateException("Failed to build RoutingAgent"));
        }

        String enrichedMessage = buildEnrichedMessage(chatRequest);
        return Flux.defer(() -> streamFrameworkMessages(agent, enrichedMessage))
                .handle((Message message, SynchronousSink<String> sink) -> {
                    if (message != null && hasText(message.getText())) {
                        sink.next(message.getText());
                    }
                })
                .switchIfEmpty(Flux.defer(() -> {
                    log.warn("Framework routing returned empty result, trigger fallback...");
                    return Flux.error(new IllegalStateException("Framework routing returned empty result"));
                }));
    }

    private Flux<Message> streamFrameworkMessages(LlmRoutingAgent agent, String userMessage) {
        try {
            return agent.streamMessages(userMessage);
        } catch (Exception ex) {
            return Flux.error(ex);
        }
    }

    private LlmRoutingAgent getOrCreateRoutingAgent() {
        LlmRoutingAgent cached = this.routingAgent;
        if (cached != null) {
            return cached;
        }

        synchronized (this) {
            if (this.routingAgent != null) {
                return this.routingAgent;
            }

            try {
                this.routingAgent = buildRoutingAgent();
                log.info("Spring AI Alibaba framework routing is enabled");
            } catch (Exception ex) {
                log.error("Failed to initialize framework routing, fallback will be used", ex);
                this.routingAgent = null;
            }

            return this.routingAgent;
        }
    }

    private LlmRoutingAgent buildRoutingAgent() {
        List<Agent> subAgents = List.of(shopAgent, productAgent, reviewAgent, generalAgent);

        return LlmRoutingAgent.builder()
                .name(ROUTER_NAME)
                .description("SmartLive auto routing agent")
                .model(frameworkChatModel)
                .stateSerializer(stateSerializer)
                .subAgents(subAgents)
                .fallbackAgent(GENERAL_AGENT_NAME)
                .systemPrompt("""
                                You are the SmartLive routing coordinator.
                                Choose the most appropriate sub-agent for each request.
                                """)
                .instruction("""
                                Routing rules:
                                1. Choose the shop agent for shops, restaurants, nearby food, store visits, blog notes and recommendations.
                                2. Choose the product agent for vouchers, products, group-buy items, prices, stock and orders.
                                3. Choose the review agent for reviews, comments, reputation and score analysis.
                                4. Choose the general agent for mixed or unclear requests.
                                """)
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}