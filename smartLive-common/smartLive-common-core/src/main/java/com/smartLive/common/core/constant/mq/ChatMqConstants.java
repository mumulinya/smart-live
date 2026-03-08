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
    /** 系统通知交换机 (Topic) */
    String SYSTEM_NOTICE_EXCHANGE = "system.notice.exchange";
    /** 系统通知队列 */
    String SYSTEM_NOTICE_QUEUE = "system.notice.queue";
    /** 系统通知路由键 */
    String SYSTEM_NOTICE_ROUTING = "system.notice.create";
}
