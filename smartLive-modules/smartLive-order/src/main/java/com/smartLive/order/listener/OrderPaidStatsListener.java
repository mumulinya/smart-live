package com.smartLive.order.listener;

import com.rabbitmq.client.Channel;
import com.smartLive.common.core.constant.RedisMqIdempotentConstants;
import com.smartLive.common.core.constant.mq.OrderMqConstants;
import com.smartLive.common.rabbitmq.domain.OrderPaidStatsMessage;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.order.service.impl.OrderServiceImpl;
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
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * 订单支付成功后的销量统计监听器。
 */
@Component
@Slf4j
public class OrderPaidStatsListener {

    @Autowired
    private OrderServiceImpl orderService;

    @Autowired
    private RedisService redisService;

    /**
     * 监听订单支付成功后的统计消息。
     * 消费端通过 Redis 幂等 Key 保证重复消息不会重复累计销量。
     *
     * @param message 统计消息
     * @param channel MQ 通道
     * @param deliveryTag 投递标记
     * @param messageId RabbitMQ 消息ID
     * @throws IOException ACK/NACK 异常
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = OrderMqConstants.ORDER_PAID_STATS_QUEUE, declare = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = OrderMqConstants.ORDER_DLX_EXCHANGE),
                            @Argument(name = "x-dead-letter-routing-key", value = OrderMqConstants.ORDER_DLQ_ROUTING_KEY)
                    }),
            exchange = @Exchange(name = OrderMqConstants.ORDER_PAID_STATS_EXCHANGE),
            key = OrderMqConstants.ORDER_PAID_STATS_ROUTING_KEY
    ))
    public void handleOrderPaidStats(OrderPaidStatsMessage message,
                                     Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                     @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        if (message == null || message.getOrderId() == null || !StringUtils.hasText(message.getMessageKey())) {
            log.warn("订单支付统计消息为空或关键字段缺失, message={}", message);
            channel.basicAck(deliveryTag, false);
            return;
        }

        String idempotentKey = RedisMqIdempotentConstants.ORDER_PREFIX + "paid-stats:" + message.getMessageKey();
        try {
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                log.info("[MQ幂等] 重复的订单支付统计消息，已跳过, key={}", idempotentKey);
                channel.basicAck(deliveryTag, false);
                return;
            }

            orderService.handleOrderPaidStats(message.getOrderId());
            log.info("订单支付统计已处理, orderId={}, messageId={}", message.getOrderId(), messageId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] 订单支付统计处理异常，清理幂等锁并触发重试, key={}", idempotentKey, e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("订单支付统计处理异常，触发本地重试", e);
        }
    }
}
