package com.smartlive.chat.listener;
import com.smartLive.common.core.constant.mq.AiAuditMqConstants;
import com.smartLive.common.core.constant.mq.ChatMqConstants;

import com.rabbitmq.client.Channel;
import com.smartlive.chat.consumer.SessionChatConsumer;
import com.smartlive.chat.dto.ChatMessageEvent;
import com.smartlive.chat.dto.SystemNoticeCreateDTO;
import com.smartlive.chat.service.ISystemNoticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import com.smartLive.common.core.constant.RedisMqIdempotentConstants;
import com.smartLive.common.redis.service.RedisService;

import java.io.IOException;

@Component
@Slf4j
public class ChatListener {

    @Autowired
    private SessionChatConsumer sessionChatConsumer;

    @Autowired
    private ISystemNoticeService systemNoticeService;

    @Autowired
    private RedisService redisService;

    /**
     * 监听所有会话队列
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = ChatMqConstants.CHAT_MESSAGE_QUEUE,
                            durable = "true",
                            arguments = {
                                    @Argument(name = "x-dead-letter-exchange", value = AiAuditMqConstants.DEAD_LETTER_EXCHANGE_NAME),
                                    @Argument(name = "x-dead-letter-routing-key", value = AiAuditMqConstants.DEAD_LETTER_ROUTING)
                            }
                    ),
                    exchange = @Exchange(
                            value = ChatMqConstants.CHAT_EXCHANGE_NAME,
                            type = ExchangeTypes.TOPIC
                    ),
                    key = ChatMqConstants.CHAT_MESSAGE_ROUTING + "*"
            )
    )
    public void consumeAllSessionMessages(ChatMessageEvent messageEvent, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        Long sessionId = messageEvent.getSessionId();
        if (messageId == null) {
            log.error("[MQ幂等] consumeAllSessionMessages消息缺失 messageId，拒绝消费");
            channel.basicNack(deliveryTag, false, false);
            return;
        }
        
        String idempotentKey = RedisMqIdempotentConstants.CHAT_PREFIX + "messageId:" + messageId;
        boolean isFirst = redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS);
        if (!isFirst) {
            log.info("[MQ幂等] 重复的消息，直接确认: {}", messageId);
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            log.info("收到会话消息: sessionId={}", sessionId);
            // 执行业务逻辑
            sessionChatConsumer.processChatMessage(messageEvent);
            // 成功：手动 ACK
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] 消息SessionId: {}消费异常，清理幂等锁并触发重试，key={}, 报错消息为{}", sessionId, idempotentKey, e.getMessage());
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("会话消息消费异常，触发本地重试", e);
        }
    }

    /**
     * 监听系统通知队列
     * 消费来自 interaction 模块（Feed 流推送）和 audit 模块（审核拒绝）的系统通知消息
     * 替代了原来的同步 RPC 调用（remoteChatService.createSystemNotice），实现跨模块异步解耦
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = ChatMqConstants.SYSTEM_NOTICE_QUEUE,
                            durable = "true",
                            // 配置死信交换机，消费失败后进入死信队列进行兜底处理
                            arguments = {
                                    @Argument(name = "x-dead-letter-exchange", value = AiAuditMqConstants.DEAD_LETTER_EXCHANGE_NAME),
                                    @Argument(name = "x-dead-letter-routing-key", value = AiAuditMqConstants.DEAD_LETTER_ROUTING)
                            }
                    ),
                    exchange = @Exchange(
                            value = ChatMqConstants.SYSTEM_NOTICE_EXCHANGE,
                            type = ExchangeTypes.TOPIC
                    ),
                    key = ChatMqConstants.SYSTEM_NOTICE_ROUTING
            )
    )
    public void consumeSystemNotice(SystemNoticeCreateDTO createDTO, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        if (messageId == null) {
            log.error("[MQ幂等] consumeSystemNotice消息缺失 messageId，拒绝消费");
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String idempotentKey = RedisMqIdempotentConstants.CHAT_PREFIX + "messageId:" + messageId;
        boolean isFirst = redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS);
        if (!isFirst) {
            log.info("[MQ幂等] 重复的系统通知消息，直接确认: {}", messageId);
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            log.info("收到系统通知消息: userId={}, action={}", createDTO.getUserId(), createDTO.getAction());
            // 调用已有的服务方法：插入 DB + WebSocket 实时推送
            systemNoticeService.createAndPush(createDTO);
            // 消费成功：手动 ACK
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] 系统通知消费异常，清理幂等锁并触发重试，key={}, userId={}, error={}", idempotentKey, createDTO.getUserId(), e.getMessage());
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("系统通知消费异常，触发本地重试", e);
        }
    }

    /**
     * 监听会话/系统通知等处理失败产生的死信队列
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = AiAuditMqConstants.DEAD_LETTER_QUEUE, durable = "true"),
            exchange = @Exchange(value = AiAuditMqConstants.DEAD_LETTER_EXCHANGE_NAME),
            key = AiAuditMqConstants.DEAD_LETTER_ROUTING
    ))
    public void handleDeadLetter(ChatMessageEvent messageEvent, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.error("死信队列收到消息: {}", messageEvent);
        // TODO: 保存到数据库异常表
        channel.basicAck(deliveryTag, false);
    }
}