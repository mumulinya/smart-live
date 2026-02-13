package com.smartLive.audit.listener;

import com.rabbitmq.client.Channel;
import com.smartLive.audit.service.IAuditService;
import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.rabbitmq.domain.AuditMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 审核模块监听器
 */
@Component
@Slf4j
public class AuditListener {

    @Autowired
    private IAuditService auditService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstants.AUDIT_QUEUE, declare = "true"),
            exchange = @Exchange(name = MqConstants.AUDIT_EXCHANGE_NAME, type = ExchangeTypes.TOPIC),
            key = MqConstants.AUDIT_ROUTING_KEY
    ))
    public void handleAuditCreate(AuditMessage auditMessage, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("收到审核任务消息: {}", auditMessage);
        try {
            auditService.handleAudit(auditMessage);
            // 手动确认
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理审核任务消息失败", e);
            try {
                // 拒绝消息，false表示不重新入队（根据业务需求，也可以设为true重新入队或进入死信队列）
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ex) {
                log.error("消息确认失败", ex);
            }
        }
    }
}
