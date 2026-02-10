package com.smartlive.chat.listener;

import com.rabbitmq.client.Channel;
import com.smartLive.common.core.constant.MqConstants;
import com.smartlive.chat.consumer.SessionChatConsumer;
import com.smartlive.chat.dto.ChatMessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class ChatListener {

    @Autowired
    private SessionChatConsumer sessionChatConsumer;

    /**
     * 监听所有会话队列
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = MqConstants.CHAT_MESSAGE_QUEUE,
                            durable = "true",
                            // ⭐ 关键修改：这里配置死信交换机和死信路由键
                            arguments = {
                                    @Argument(name = "x-dead-letter-exchange", value = MqConstants.DEAD_LETTER_EXCHANGE_NAME),
                                    @Argument(name = "x-dead-letter-routing-key", value = MqConstants.DEAD_LETTER_ROUTING)
                            }
                    ),
                    exchange = @Exchange(
                            value = MqConstants.CHAT_EXCHANGE_NAME,
                            type = ExchangeTypes.TOPIC // 一定要指定为 topic
                    ),
                    key = MqConstants.CHAT_MESSAGE_ROUTING + "*"            // 匹配所有 session.chat.xxx 的路由
            )
    )
    public void consumeAllSessionMessages(ChatMessageEvent messageEvent, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        Long sessionId = messageEvent.getSessionId();
        try {
            log.info("✅ 收到会话消息: sessionId={}", sessionId);

            // 执行你的业务逻辑
            sessionChatConsumer.processChatMessage(messageEvent);
            // 模拟业务逻辑...
//            int i = 1 / 0; // 模拟异常

            // 成功：手动 ACK
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error(" 消息SessionId: {}消费失败，即将进入死信队列,报错消息为{} ", sessionId, e.getMessage());

            // 失败：手动 NACK
            // 参数1：Tag
            // 参数2：multiple (是否批量) -> false
            // 参数3：requeue (是否重回原队列) -> ⭐ false (设为 false 才会进死信队列)
            channel.basicNack(deliveryTag, false, false);
        }
    }
    /**
     * 监听死信队列
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConstants.DEAD_LETTER_QUEUE, durable = "true"), // 死信队列名
            exchange = @Exchange(value = MqConstants.DEAD_LETTER_EXCHANGE_NAME),
            key = MqConstants.DEAD_LETTER_ROUTING
    ))
    public void handleDeadLetter(ChatMessageEvent messageEvent, Channel channel,@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.error("🚨 死信队列收到消息: {}", messageEvent);
        // TODO: 保存到数据库异常表
        channel.basicAck(deliveryTag, false);
    }
}