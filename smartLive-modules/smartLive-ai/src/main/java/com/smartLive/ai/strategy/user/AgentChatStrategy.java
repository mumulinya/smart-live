package com.smartLive.ai.strategy.user;

import com.smartLive.ai.entity.request.AIChatRequest;
import reactor.core.publisher.Flux;

/**
 * 智能体聊天策略类。
 */
public interface AgentChatStrategy {

    /**
     * 返回字符串数据流。
     */
    Flux<String> streamChat(AIChatRequest chatRequest);

    /**
     * 解析聊天 ID。
     */
    default String resolveChatId(AIChatRequest request) {
        if (request == null) {
            return "anonymous";
        }
        if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
            return request.getSessionId();
        }
        if (request.getUserId() != null && !request.getUserId().isBlank()) {
            return "user::" + request.getUserId();
        }
        return "anonymous";
    }

    /**
     * 构建enriched消息。
     */
    default String buildEnrichedMessage(AIChatRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("User message: ").append(request.getMessage());
        if(request.getUserId()!= null){
            sb.append("\n(User ID: ").append(request.getUserId()).append(")");
        }
        if (request.getDistrict() != null && !request.getDistrict().trim().isEmpty()) {
            sb.append("\n(User district: ").append(request.getDistrict());
            if (request.getX() != null && request.getY() != null) {
                sb.append(", coordinates: ").append(request.getX()).append(",").append(request.getY());
            }
            sb.append(")");
        } else if (request.getX() != null && request.getY() != null) {
            sb.append("\n(User coordinates: ").append(request.getX()).append(",").append(request.getY()).append(")");
        }
        return sb.toString();
    }
}
