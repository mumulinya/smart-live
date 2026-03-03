package com.smartLive.audit.listener;
import com.smartLive.common.core.constant.mq.AiAuditMqConstants;
import com.smartLive.common.core.constant.RedisMqIdempotentConstants;

import com.rabbitmq.client.Channel;
import com.smartLive.audit.service.IAuditService;
import com.smartLive.common.rabbitmq.domain.AuditMessage;
import com.smartLive.common.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Audit listener
 */
@Component
@Slf4j
public class AuditListener {

    @Autowired
    private IAuditService auditService;

    @Autowired
    private RedisService redisService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = AiAuditMqConstants.AUDIT_QUEUE, declare = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = AiAuditMqConstants.AUDIT_DEAD_LETTER_EXCHANGE_NAME),
                            @Argument(name = "x-dead-letter-routing-key", value = AiAuditMqConstants.AUDIT_DEAD_LETTER_ROUTING)
                    }
            ),
            exchange = @Exchange(name = AiAuditMqConstants.AUDIT_EXCHANGE_NAME, type = ExchangeTypes.TOPIC),
            key = AiAuditMqConstants.AUDIT_ROUTING_KEY
    ))
    public void handleAuditCreate(AuditMessage auditMessage, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        // 构建幂等 key: audit:bizType:bizId
        String bizKey = auditMessage.getBizType() + ":" + auditMessage.getBizId();
        String idempotentKey = RedisMqIdempotentConstants.AUDIT_PREFIX + bizKey;

        try {
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                log.info("[MQ幂等] 重复消息，已跳过，key={}", idempotentKey);
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("[MQ幂等] 首次消费，key={}", idempotentKey);

            auditService.handleAudit(auditMessage);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] 审核消息处理失败，key={}，将进入死信", idempotentKey, e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ex) {
                log.error("消息确认失败", ex);
            }
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = AiAuditMqConstants.AUDIT_DEAD_LETTER_QUEUE, durable = "true"),
            exchange = @Exchange(value = AiAuditMqConstants.AUDIT_DEAD_LETTER_EXCHANGE_NAME),
            key = AiAuditMqConstants.AUDIT_DEAD_LETTER_ROUTING
    ))
    public void handleAuditDeadLetter(AuditMessage auditMessage, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.error("audit dead letter received: {}", auditMessage);
        channel.basicAck(deliveryTag, false);
    }
}
