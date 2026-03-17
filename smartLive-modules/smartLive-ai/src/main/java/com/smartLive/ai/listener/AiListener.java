package com.smartLive.ai.listener;

import com.smartLive.common.core.constant.mq.AiAuditMqConstants;
import com.smartLive.common.core.constant.RedisMqIdempotentConstants;
import com.smartLive.ai.entity.request.AIGenerateRequest;
import com.smartLive.ai.service.ai.AIReviewGenerateService;
import com.smartLive.common.redis.service.RedisService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Argument;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * AI 监听器。
 */
@Component
@Slf4j
public class AiListener {

    @Autowired
    private AIReviewGenerateService aiReviewGenerateService;

    @Autowired
    private RedisService redisService;

    /**
     * 监听 AI 创建评论队列，处理 AI 生成评论任务。
     */
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
    /**
     * 处理 AI 创建评论消息。
     */
    public void handleAiCreateComment(List<AIGenerateRequest> list, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        if (list == null || list.isEmpty()) {
            channel.basicAck(deliveryTag, false);
            return;
        }
        if (messageId == null || messageId.isEmpty()) {
            log.error("[MQ幂等] 消息缺失 messageId，拒绝消费. list size={}", list.size());
            channel.basicNack(deliveryTag, false, false);
            return;
        }
        
        String bizKey = "messageId:" + messageId;
        String idempotentKey = RedisMqIdempotentConstants.AI_PREFIX + bizKey;

        try {
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                log.info("[MQ幂等] 重复消息，已跳过，key={}", idempotentKey);
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("[MQ幂等] 首次消费，key={}", idempotentKey);

            list.forEach(item -> {
                if (item.getSourceIds() != null) {
                    item.setSourceIds(item.getSourceIds().subList(0, Math.min(4, item.getSourceIds().size())));
                }
            });
            log.info("ai comment generation request received, size={}", list.size());
            aiReviewGenerateService.aiCreateReview(list);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] AI生成评论处理异常，清理幂等锁并触发重试，key={}", idempotentKey, e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("AI生成评论处理异常，触发本地重试", e);
        }
    }

    /**
     * 监听 AI 执行异常后的死信队列。
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = AiAuditMqConstants.AI_DEAD_LETTER_QUEUE, durable = "true"),
            exchange = @Exchange(value = AiAuditMqConstants.AI_DEAD_LETTER_EXCHANGE_NAME),
            key = AiAuditMqConstants.AI_DEAD_LETTER_ROUTING
    ))
    /**
     * 处理 AI 死信消息。
     */
    public void handleAiDeadLetter(org.springframework.amqp.core.Message message) {
        log.error("ai dead letter received: {}", new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8));
    }
}
