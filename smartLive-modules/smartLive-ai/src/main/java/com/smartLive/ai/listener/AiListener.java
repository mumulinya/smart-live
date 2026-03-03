package com.smartLive.ai.listener;

import com.smartLive.common.core.constant.mq.AiAuditMqConstants;
import com.smartLive.ai.entity.request.AIGenerateRequest;
import com.smartLive.ai.strategy.handlers.CommentHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Argument;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class AiListener {

    @Autowired
    private CommentHandler commentHandler;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = AiAuditMqConstants.AI_COMMENT_QUEUE, declare = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = AiAuditMqConstants.AI_DEAD_LETTER_EXCHANGE_NAME),
                            @Argument(name = "x-dead-letter-routing-key", value = AiAuditMqConstants.AI_DEAD_LETTER_ROUTING)
                    }
            ),
            exchange = @Exchange(name = AiAuditMqConstants.AI_EXCHANGE_NAME),
            key = AiAuditMqConstants.AI_COMMENT_ROUTING
    ))
    public void handleAiCreateComment(List<AIGenerateRequest> list) {
        list.forEach(item -> item.setSourceIds(item.getSourceIds().subList(0, 4)));
        log.info("ai comment generation request received, size={}", list == null ? 0 : list.size());
        commentHandler.aiCreateComment(list);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = AiAuditMqConstants.AI_DEAD_LETTER_QUEUE, durable = "true"),
            exchange = @Exchange(value = AiAuditMqConstants.AI_DEAD_LETTER_EXCHANGE_NAME),
            key = AiAuditMqConstants.AI_DEAD_LETTER_ROUTING
    ))
    public void handleAiDeadLetter(org.springframework.amqp.core.Message message) {
        log.error("ai dead letter received: {}", new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8));
    }
}
