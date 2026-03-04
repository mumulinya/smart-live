package com.smartLive.ai.listener;

import com.smartLive.common.core.constant.mq.AiAuditMqConstants;
import com.smartLive.common.core.constant.RedisMqIdempotentConstants;
import com.smartLive.ai.entity.request.AIGenerateRequest;
import com.smartLive.ai.strategy.handlers.CommentHandler;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.util.DigestUtils;

@Component
@Slf4j
public class AiListener {

    @Autowired
    private CommentHandler commentHandler;

    @Autowired
    private RedisService redisService;

    /**
     * 监听AI创建评论队列，处理AI生成评论任务
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
            commentHandler.aiCreateComment(list);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] AI生成评论处理异常，清理幂等锁并触发重试，key={}", idempotentKey, e);
            redisService.deleteObject(idempotentKey);
            // 抛出异常以触发 Spring Retry 机制。当重试次数耗尽后，
            // 默认的 RejectAndDontRequeueRecoverer 会将消息投入死信队列（若配置了死信交换机）
            throw new RuntimeException("AI生成评论处理异常，触发本地重试", e);
        }
    }

    /**
     * 监听AI执行异常后的死信队列
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = AiAuditMqConstants.AI_DEAD_LETTER_QUEUE, durable = "true"),
            exchange = @Exchange(value = AiAuditMqConstants.AI_DEAD_LETTER_EXCHANGE_NAME),
            key = AiAuditMqConstants.AI_DEAD_LETTER_ROUTING
    ))
    public void handleAiDeadLetter(org.springframework.amqp.core.Message message) {
        log.error("ai dead letter received: {}", new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8));
    }
}
