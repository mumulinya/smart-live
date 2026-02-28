package com.smartLive.common.core.constant.mq;

/**
 * 聊天室（私聊/在线状态）MQ 常量
 */
public interface ChatMqConstants {
    // 私聊交换�?
    String CHAT_EXCHANGE_NAME = "chat.topic";
    String CHAT_MESSAGE_QUEUE = "chat.message.queue";
    String CHAT_MESSAGE_ROUTING = "chat.session.";
}
