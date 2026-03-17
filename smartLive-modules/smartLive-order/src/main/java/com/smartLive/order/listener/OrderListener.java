package com.smartLive.order.listener;
import com.smartLive.common.core.constant.mq.OrderMqConstants;
import com.smartLive.common.core.constant.RedisMqIdempotentConstants;

import com.rabbitmq.client.Channel;
import com.smartLive.common.core.constant.OrderStatusConstants;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.order.domain.Order;
import com.smartLive.order.service.impl.OrderServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import java.io.IOException;

/**
 * 订单消息监听器，处理下单、支付延迟、取消回滚等消息。
 */
@Component
@Slf4j
public class OrderListener {

    @Autowired
    private OrderServiceImpl orderService;

    @Autowired
    private MqMessageSendUtils mqMessageSendUtils;

    @Autowired
    private RedisService redisService;

    /**
     * 监听秒杀订单下单请求，执行库存扣减与订单创建
     */
    @RabbitListener(bindings=@QueueBinding(
            value = @Queue(name = OrderMqConstants.ORDER_SECKILL_QUEUE,
                    declare = "true",
                    //配置死信交换机和死信路由键
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = OrderMqConstants.ORDER_DLX_EXCHANGE),
                            @Argument(name = "x-dead-letter-routing-key", value = OrderMqConstants.ORDER_DLQ_ROUTING_KEY),
                            //设置惰性队列
                            @Argument(name = "x-queue-mode", value = "lazy")
                    }
            ),
            exchange = @Exchange(name = OrderMqConstants.ORDER_DIRECT_EXCHANGE),
            key = OrderMqConstants.ORDER_SECKILL_ROUTING_KEY
    ))
    public void handleSeckillOrder(Order order, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        if (order == null || order.getId() == null) {
            log.warn("秒杀订单消息为空或无ID");
            channel.basicAck(deliveryTag, false);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            log.error("[MQ幂等] 秒杀订单消息缺失 messageId，拒绝消费. orderId={}", order.getId());
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String bizKey = "seckill:messageId:" + messageId;
        String idempotentKey = RedisMqIdempotentConstants.ORDER_PREFIX + bizKey;

        try {
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                log.info("[MQ幂等] 重复消息，已跳过，key={}", idempotentKey);
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("[MQ幂等] 首次消费，key={}", idempotentKey);

            // 数据库兜底：判断当前订单是否重复创建
            if (orderService.getById(order.getId()) != null) {
                log.info("[MQ幂等] 订单已存在（DB兜底），orderId={}", order.getId());
                channel.basicAck(deliveryTag, false);
                return;
            }
            orderService.handleOrder(order);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] 秒杀订单处理异常，清理幂等锁并触发重试，key={}", idempotentKey, e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("秒杀订单处理异常，触发本地重试", e);
        }
    }

    /**
     * 监听普通订单下单请求，完成普通订单创建流转
     */
    @RabbitListener(bindings=@QueueBinding(
            value = @Queue(name = OrderMqConstants.ORDER_BUY_QUEUE,
                    declare = "true",
                    //配置死信交换机和死信路由键
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = OrderMqConstants.ORDER_DLX_EXCHANGE),
                            @Argument(name = "x-dead-letter-routing-key", value = OrderMqConstants.ORDER_DLQ_ROUTING_KEY)
                    }
            ),
            exchange = @Exchange(name = OrderMqConstants.ORDER_DIRECT_EXCHANGE),
            key = OrderMqConstants.ORDER_BUY_ROUTING_KEY
    ))
    public void handleBuyOrder(Order order, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        if (order == null || order.getId() == null) {
            log.warn("普通订单消息为空或无ID");
            channel.basicAck(deliveryTag, false);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            log.error("[MQ幂等] 普通订单消息缺失 messageId，拒绝消费. orderId={}", order.getId());
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String bizKey = "buy:messageId:" + messageId;
        String idempotentKey = RedisMqIdempotentConstants.ORDER_PREFIX + bizKey;


        try {
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                log.info("[MQ幂等] 重复消息，已跳过，key={}", idempotentKey);
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("[MQ幂等] 首次消费，key={}", idempotentKey);

            log.info("开始处理订单信息: {}", order);
            // 数据库兜底：判断当前订单是否重复创建
            if (orderService.getById(order.getId()) != null) {
                log.info("[MQ幂等] 订单已存在（DB兜底），orderId={}", order.getId());
                channel.basicAck(deliveryTag, false);
                return;
            }
            //创建订单
            boolean save = orderService.save(order);
            if (!save) {
                log.error("创建订单失败");
                redisService.deleteObject(idempotentKey);
                throw new RuntimeException("普通订单保存失败，触发本地重试");
            }

            // 创建成功，删除缓存占位符
            redisService.deleteObject("order:status:" + order.getId());

            //发送延迟消息，检测订单支付状态
            mqMessageSendUtils.sendMqMessage(OrderMqConstants.ORDER_DELAY_EXCHANGE, OrderMqConstants.ORDER_DELAY_ROUTING_KEY, order.getId(), (OrderMqConstants.DELAY_TIME));
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] 普通订单处理异常，清理幂等锁并触发重试, orderId={}, key={}", order.getId(), idempotentKey, e);
            redisService.deleteObject(idempotentKey);
            redisService.deleteObject("order:status:" + order.getId());
            throw new RuntimeException("普通订单处理异常，触发本地重试", e);
        }
    }

    /**
     * 监听订单支付延迟检查队列，倒计时结束后检查是否完成付款
     */
    @RabbitListener(bindings=@QueueBinding(
            value = @Queue(name = OrderMqConstants.ORDER_DELAY_QUEUE),
            exchange = @Exchange(name = OrderMqConstants.ORDER_DELAY_EXCHANGE,
                    type = "x-delayed-message", // 使用 x-delayed-message 类型交换机
                    durable = "true",
                    arguments = @Argument(name = "x-delayed-type", value = "direct") // 指定路由类型
                    ),
            key = OrderMqConstants.ORDER_DELAY_ROUTING_KEY
    ))
    public void handlePayOrder(Long id, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        if (id == null) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            log.error("[MQ幂等] 支付延迟消息缺失 messageId，拒绝消费. id={}", id);
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String bizKey = "delay:messageId:" + messageId;
        String idempotentKey = RedisMqIdempotentConstants.ORDER_PREFIX + bizKey;


        try {
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                log.info("[MQ幂等] 重复消息，已跳过，key={}", idempotentKey);
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("[MQ幂等] 首次消费，key={}", idempotentKey);

            Order order = orderService.getById(id);
            //检测订单状态，判断订单是否支付
            if (order == null || order.getStatus() == OrderStatusConstants.PAID) {
                log.info("订单不存在或者订单已经支付");
                channel.basicAck(deliveryTag, false);
                return;
            }
            //订单未支付
            if (order.getStatus() == OrderStatusConstants.UNPAID) {
                log.info("订单未支付，取消订单");
                //取消订单,恢复库存
                orderService.cancel(id);
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] 支付延迟消息处理异常，清理幂等锁并触发重试，key={}", idempotentKey, e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("支付延迟处理异常，触发本地重试", e);
        }
    }

    /**
     * 监听订单取消/回滚队列 (商品扣库失败时调用)
     */
    @RabbitListener(bindings=@QueueBinding(
            value = @Queue(name = OrderMqConstants.ORDER_CANCEL_QUEUE, declare = "true",
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = OrderMqConstants.ORDER_DLX_EXCHANGE),
                            @Argument(name = "x-dead-letter-routing-key", value = OrderMqConstants.ORDER_DLQ_ROUTING_KEY)
                    }
            ),
            exchange = @Exchange(name = OrderMqConstants.ORDER_CANCEL_EXCHANGE),
            key = OrderMqConstants.ORDER_CANCEL_ROUTING_KEY
    ))
    public void handleCancelOrder(Long orderId, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        if (orderId == null) {
            log.warn("回滚取消订单消息为空或无ID");
            channel.basicAck(deliveryTag, false);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            log.error("[MQ幂等] 取消订单消息缺失 messageId，拒绝消费. orderId={}", orderId);
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String bizKey = "cancel:messageId:" + messageId;
        String idempotentKey = RedisMqIdempotentConstants.ORDER_PREFIX + bizKey;

        try {
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                log.info("[MQ幂等] 回滚订单请求为重复消息，已跳过，key={}", idempotentKey);
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("[MQ幂等] 首次执行订单回滚，key={}, orderId={}", idempotentKey, orderId);

            Order order = orderService.getById(orderId);
            if (order != null && order.getStatus() != OrderStatusConstants.CANCELLED) {
                log.warn("商品库存扣减失败，系统触发强行取消订单!");
                // 取消订单
                orderService.cancel(orderId);
            } else {
                log.info("订单不存在或已处于取消状态");
            }
            
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] 回滚订单处理异常，清理幂等锁并触发重试，key={}", idempotentKey, e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("回滚订单处理异常，触发本地重试", e);
        }
    }

    /**
     * 监听死信队列
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = OrderMqConstants.ORDER_DLQ_QUEUE, durable = "true"), // 死信队列名
            exchange = @Exchange(value = OrderMqConstants.ORDER_DLX_EXCHANGE),
            key = OrderMqConstants.ORDER_DLQ_ROUTING_KEY
    ))
    public void handleDeadLetter(Order order, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.error("死信队列收到订单信息为: {}", order);
        // 待办：保存到数据库异常表
        channel.basicAck(deliveryTag, false);
    }
}
