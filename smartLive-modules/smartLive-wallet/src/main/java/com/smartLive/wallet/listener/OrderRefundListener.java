package com.smartLive.wallet.listener;

import com.smartLive.common.core.constant.PayTypeConstants;
import com.smartLive.common.core.constant.RedisMqIdempotentConstants;
import com.smartLive.common.core.constant.mq.OrderMqConstants;
import com.smartLive.common.core.exception.ServiceException;
import com.smartLive.common.rabbitmq.domain.OrderRefundMessage;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.order.api.RemoteOrderService;
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

    @Autowired
    private RemoteOrderService remoteOrderService;

    /**
     * 监听订单退款消息。
     * 余额退款默认走 RPC，这里主要承接第三方退款的异步确认链路。
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

            if (message.getPayType() == null) {
                throw new ServiceException("退款消息缺少支付方式");
            }

            if (message.getPayType().equals(PayTypeConstants.BALANCE)) {
                // 兼容历史消息：即便余额退款已经切到 RPC，这里仍保留兜底处理能力。
                walletService.refundOrder(message.getUserId(), message.getAmount(), message.getOrderId().toString());
                confirmRefundSuccess(message.getOrderId(), "balance");
                log.info("订单余额退款处理完成, orderId={}, userId={}, amount={}", message.getOrderId(), message.getUserId(), message.getAmount());
            } else {
                // 真实环境这里需要调用支付宝/微信退款接口，并以回调/查询结果作为最终成功依据。
                log.info("收到第三方退款请求，当前按模拟成功回执处理, orderId={}, payType={}", message.getOrderId(), message.getPayType());
                confirmRefundSuccess(message.getOrderId(), "third-party");
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] 退款消息处理异常，清理幂等锁并触发重试，key={}", idempotentKey, e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("退款消息处理异常，触发本地重试", e);
        }
    }

    /**
     * 退款执行成功后回调订单模块确认最终状态。
     */
    private void confirmRefundSuccess(Long orderId, String refundChannel) {
        Integer result = remoteOrderService.confirmRefundSuccess(orderId);
        if (result == null || result <= 0) {
            throw new ServiceException("订单退款成功确认失败");
        }
        log.info("订单退款成功确认完成, orderId={}, refundChannel={}", orderId, refundChannel);
    }
}
