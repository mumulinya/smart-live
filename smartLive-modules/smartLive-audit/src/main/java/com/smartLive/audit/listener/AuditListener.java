package com.smartLive.audit.listener;

import com.rabbitmq.client.Channel;
import com.smartLive.audit.service.IAuditService;
import com.smartLive.common.core.constant.RedisMqIdempotentConstants;
import com.smartLive.common.core.constant.mq.AiAuditMqConstants;
import com.smartLive.common.rabbitmq.domain.AuditMessage;
import com.smartLive.common.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
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

@Component
@Slf4j
public class AuditListener {

    @Autowired
    private IAuditService auditService;

    @Autowired
    private RedisService redisService;

    /**
     * 监听内容审核队列，处理文字、图片内容的合规性机审/人审流程
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = AiAuditMqConstants.AUDIT_QUEUE, declare = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = AiAuditMqConstants.AUDIT_DLX_EXCHANGE),
                            @Argument(name = "x-dead-letter-routing-key", value = AiAuditMqConstants.AUDIT_DLQ_ROUTING_KEY)
                    }
            ),
            exchange = @Exchange(name = AiAuditMqConstants.AUDIT_DIRECT_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = AiAuditMqConstants.AUDIT_ROUTING_KEY
    ))
    public void handleAuditCreate(
            AuditMessage auditMessage,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {

        if (auditMessage == null) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            log.error("[MQ幂等] Audit message missing messageId, reject consume");
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String idempotentKey = RedisMqIdempotentConstants.AUDIT_PREFIX + "messageId:" + messageId;

        try {
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                log.info("[MQ幂等] Duplicate message skipped, key={}", idempotentKey);
                channel.basicAck(deliveryTag, false);
                return;
            }

            auditService.handleAudit(auditMessage);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] Audit consume failed, clear idempotent key and trigger retry, key={}", idempotentKey, e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("Audit message processing failed, trigger local retry", e);
        }
    }

    /**
     * 处理审核死信消息
     *
     * @param auditMessage 审核消息
     * @param channel      MQ 通道
     * @param deliveryTag  投递标识
     * @throws IOException IO 异常
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = AiAuditMqConstants.AUDIT_DLQ_QUEUE, durable = "true"),
            exchange = @Exchange(value = AiAuditMqConstants.AUDIT_DLX_EXCHANGE),
            key = AiAuditMqConstants.AUDIT_DLQ_ROUTING_KEY
    ))
    public void handleAuditDeadLetter(AuditMessage auditMessage, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.error("audit dead letter received: {}", auditMessage);
        channel.basicAck(deliveryTag, false);
    }
}
