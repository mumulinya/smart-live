package com.smartLive.common.core.constant.mq;

/**
 * MQ Constants for Chat and System Notice
 */
public interface ChatMqConstants {
    // Private Chat Exchange
    String CHAT_DIRECT_EXCHANGE = "chat.direct.exchange";
    String CHAT_MESSAGE_QUEUE = "chat.message.queue";
    String CHAT_MESSAGE_ROUTING = "chat.session.";

    // ==================== System Notice MQ Constants ====================
    /** System Notice Exchange (Topic) */
    String SYSTEM_NOTICE_EXCHANGE = "system.notice.exchange";
    /** System Notice Queue */
    String SYSTEM_NOTICE_QUEUE = "system.notice.queue";
    /** System Notice Routing Key */
    String SYSTEM_NOTICE_ROUTING = "system.notice.create";
}
