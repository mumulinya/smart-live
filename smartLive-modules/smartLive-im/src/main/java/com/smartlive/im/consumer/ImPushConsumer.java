package com.smartlive.im.consumer;
import com.smartLive.common.core.constant.mq.ChatMqConstants;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlive.im.handler.NettyChatHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;
import com.smartLive.common.core.constant.mq.AiAuditMqConstants;
import com.smartLive.common.core.constant.RedisMqIdempotentConstants;
import com.smartLive.common.redis.service.RedisService;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import java.io.IOException;

import java.util.Map;

@Slf4j
@Component
public class ImPushConsumer {

    @Autowired
    private NettyChatHandler nettyChatHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisService redisService;

    /**
     * 监听全局 WebSocket 消息推送队列
     * 负责将各模块产生的实时事件通过 Netty 通道推送给指定的用户设备
     * 路由键: im.push.user
     * 数据格式: { "userId": 123, "json": "{...}" }
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(
                    name = "im.push.queue",
                    durable = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = AiAuditMqConstants.DEAD_LETTER_EXCHANGE_NAME),
                            @Argument(name = "x-dead-letter-routing-key", value = AiAuditMqConstants.DEAD_LETTER_ROUTING)
                    }
            ),
            exchange = @Exchange(name = ChatMqConstants.CHAT_DIRECT_EXCHANGE, type = "topic"),
            key = "im.push.user"
    ))
    public void handlePushMessage(Map<String, Object> map, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        if (messageId == null) {
            log.error("[MQ幂等] IM推消息缺失 messageId，拒绝消费");
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String idempotentKey = RedisMqIdempotentConstants.IM_PREFIX + "messageId:" + messageId;
        boolean isFirst = redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS);
        if (!isFirst) {
            log.info("[MQ幂等] 重复的IM消息，直接确认: {}", messageId);
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            Long userId = Long.valueOf(map.get("userId").toString());
            String json = (String) map.get("json");

            if (NettyChatHandler.isUserOnline(userId)) {
                NettyChatHandler.pushMessageToUser(userId, json);
                log.info("MQ推送消息给用户: {}", userId);
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] MQ推送消息处理异常，清理幂等锁并触发重试，key={}, 消息内容: {}", idempotentKey, map, e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("MQ推送消息处理异常，触发本地重试", e);
        }
    }
}
