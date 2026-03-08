package com.smartLive.ai.service.chat;

import com.smartLive.ai.entity.request.AIChatRequest;
import reactor.core.publisher.Flux;

/**
 * 统一的 AI 聊天策略接口。
 * 用于抹平不同底层 Agent 框架（如 Spring AI Alibaba, 原生 Spring AI 等）之间的调用差异。
 */
public interface AgentChatStrategy {

    /**
     * 流式生成聊天响应内容。
     *
     * @param chatRequest 用户输入的消息文本
     * @param chatId      会话ID，用于保持记忆上下文
     * @return 响应字符串的 Flux 流
     */
    Flux<String> streamChat(AIChatRequest chatRequest, String chatId);

    /**
     * 构建包含用户位置信息的增强版消息内容
     */
    default String buildEnrichedMessage(AIChatRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户问题: ").append(request.getMessage());
        if (request.getDistrict() != null && !request.getDistrict().trim().isEmpty()) {
            sb.append("\n(用户所在地区： ").append(request.getDistrict());
            if (request.getX() != null && request.getY() != null) {
                sb.append("，\n用户当前位置的经度").append(request.getX()).append(",用户当前位置的纬度:").append(request.getY());
            }
            sb.append(")");
        } else if (request.getX() != null && request.getY() != null) {
            sb.append("，\n用户当前位置的经度").append(request.getX()).append(",用户当前位置的纬度:").append(request.getY());
        }
        return sb.toString();
    }
}
