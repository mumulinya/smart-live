package com.smartLive.common.core.constant.mq;

/**
 * AI 与审核模�?MQ 常量
 */
public interface AiAuditMqConstants {
    // AI 评论生成交换�?
    String AI_EXCHANGE_NAME = "ai.direct";
    String AI_COMMENT_QUEUE = "ai.comment.queue";
    String AI_COMMENT_ROUTING = "ai.comment.create";
    String AI_DEAD_LETTER_EXCHANGE_NAME = "ai.dead.letter.direct";
    String AI_DEAD_LETTER_QUEUE = "ai.dead.letter.queue";
    String AI_DEAD_LETTER_ROUTING = "ai.dead.letter.routing";

    // 审核系统交换�?
    String AUDIT_EXCHANGE_NAME = "audit.topic";
    String AUDIT_QUEUE = "audit.queue";
    String AUDIT_ROUTING_KEY = "audit.create";
    String AUDIT_DEAD_LETTER_EXCHANGE_NAME = "audit.dead.letter.direct";
    String AUDIT_DEAD_LETTER_QUEUE = "audit.dead.letter.queue";
    String AUDIT_DEAD_LETTER_ROUTING = "audit.dead.letter.routing";

    // 死信交换机（用于各种处理回调�?
    String DEAD_LETTER_EXCHANGE_NAME = "dead.letter.direct";
    String DEAD_LETTER_QUEUE = "dead.letter.queue";
    String DEAD_LETTER_ROUTING = "dead.letter.routing";
}
