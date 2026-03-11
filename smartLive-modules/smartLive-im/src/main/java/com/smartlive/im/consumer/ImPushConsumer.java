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

/**
 * 后端消息实时推送消费者
 * 监听 RabbitMQ 中的推送队列，根据消息指令通过 Netty 集群节点的活跃通道推送至用户终端。
 * 该消费者解决了后端业务模块（如支付模块、审核模块）与前端 WebSocket 活跃连接的解耦通信。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
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
     * 负责将全系统产生的实时事件（如账户变动、新私聊、系统通知）推送到当前节点的 Netty 活跃通道。
     * 
     * 1. 幂等性控制：利用 Redis 原子锁防止消息重复投递导致的 UI 抖动或由于网路重试导致的多次弹窗问题。
     * 2. 死信队列集成：推送失败且无法自愈时自动进入死信，便于后续排查。
     * 
     * @param map 包含推送指令的负载（需包含 userId 和内容 JSON）
     * @param channel RabbitMQ 通道对象
     * @param deliveryTag 交付标签，用于手动 ACK
     * @param messageId 全局唯一消息 ID，用于幂等
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
            log.error("[MQ推送幂等错误] IM 推送消息缺失 messageId，出于安全考虑拒绝消费并退信");
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        // 幂等逻辑校验：保证每条推送指令仅被集群中一个节点成功执行一次
        String idempotentKey = RedisMqIdempotentConstants.IM_PREFIX + "messageId:" + messageId;
        boolean isFirst = redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS);
        if (!isFirst) {
            log.info("[MQ推送幂等拦截] 捕获到重复投递的推送消息，已标记为幂等处理: {}", messageId);
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            Long userId = Long.valueOf(map.get("userId").toString());
            String json = (String) map.get("json");

            // 检查接收方连接是否在当前服务器物理节点上
            if (NettyChatHandler.isUserOnline(userId)) {
                NettyChatHandler.pushMessageToUser(userId, json);
                log.info("用户 {} 实时连接命在本节点，成功推送 WebSocket 载荷", userId);
            } else {
                // 如果用户不在本节点上线，由于 Exchange 类型为 Topic 且 Key 为 im.push.user，
                // 集群架构下其他节点也会订阅此消息并进行同样的本地匹配检查。
                log.debug("用户 {} 未在本节点物理上线，略过此消息", userId);
            }
            // 确认消费成功
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[IM 推送异常] 业务推送逻辑崩溃，清除幂等标记以触发 RabbitMQ 自动重试机制: {}", map, e);
            redisService.deleteObject(idempotentKey);
            // 抛出运行时异常，RabbitMQ 监听器将触发异常重试策略
            throw new RuntimeException("IM 消息推送子服务不可用", e);
        }
    }
}
