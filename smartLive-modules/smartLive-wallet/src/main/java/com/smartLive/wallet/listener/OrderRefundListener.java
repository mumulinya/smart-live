package com.smartLive.wallet.listener;

import com.smartLive.common.core.constant.RedisMqIdempotentConstants;
import com.smartLive.common.core.constant.mq.OrderMqConstants;
import com.smartLive.common.rabbitmq.domain.OrderRefundMessage;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.wallet.service.IWalletService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 订单退款监听器
 * 监听订单取消/退款消息，恢复用户钱包余额
 */
@Component
@Slf4j
public class OrderRefundListener {

    @Autowired
    private IWalletService walletService;

    @Autowired
    private RedisService redisService;

    /**
     * 监听订单退款消息，恢复用户余额
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = OrderMqConstants.ORDER_REFUND_QUEUE, declare = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = OrderMqConstants.ORDER_DLX_EXCHANGE),
                            @Argument(name = "x-dead-letter-routing-key", value = OrderMqConstants.ORDER_DLQ_ROUTING_KEY)
                    }
            ),
            exchange = @Exchange(name = OrderMqConstants.ORDER_REFUND_EXCHANGE),
            key = OrderMqConstants.ORDER_REFUND_ROUTING_KEY
    ))
    public void handleOrderRefund(OrderRefundMessage message, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                  @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        if (message == null || message.getOrderId() == null) {
            log.warn("退款消息为空或无订单ID");
            channel.basicAck(deliveryTag, false);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            log.error("[MQ幂等] 退款消息缺失 messageId，拒绝消费. orderId={}", message.getOrderId());
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String bizKey = "refund:messageId:" + messageId;
        String idempotentKey = RedisMqIdempotentConstants.ORDER_PREFIX + bizKey;

        try {
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                log.info("[MQ幂等] 重复消息，已跳过，key={}", idempotentKey);
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("[MQ幂等] 首次消费退款消息，key={}, orderId={}", idempotentKey, message.getOrderId());

            walletService.refundOrder(message.getUserId(), message.getAmount(), message.getOrderId().toString());
            log.info("订单退款余额恢复成功, orderId={}, userId={}, amount={}", message.getOrderId(), message.getUserId(), message.getAmount());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] 退款消息处理异常，清理幂等锁并触发重试，key={}", idempotentKey, e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("退款消息处理异常，触发本地重试", e);
        }
    }
}
