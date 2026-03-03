package com.smartLive.order.listener;
import com.smartLive.common.core.constant.mq.OrderMqConstants;

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

@Component
@Slf4j
public class OrderListener {

    @Autowired
    private OrderServiceImpl orderService;

    
    @Autowired
    private MqMessageSendUtils mqMessageSendUtils;

    @Autowired
    private RedisService redisService;
    
    //秒杀订单监听
    @RabbitListener(bindings=@QueueBinding(
            value = @Queue(name = OrderMqConstants.ORDER_SECKILL_QUEUE,
                    declare = "true",
                    //配置死信交换机和死信路由键
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = OrderMqConstants.ORDER_DEAD_LETTER_EXCHANGE_NAME),
                            @Argument(name = "x-dead-letter-routing-key", value = OrderMqConstants.ORDER_DEAD_LETTER_ROUTING),
                            //设置惰性队列
                            @Argument(name = "x-queue-mode", value = "lazy")
                    }
            ),
            exchange = @Exchange(name = OrderMqConstants.ORDER_EXCHANGE_NAME),
            key = OrderMqConstants.ORDER_SECKILL_ROUTING
    ))
    public void handleSeckillOrder(Order order, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            //判断当前订单是否重复创建
            if (orderService.getById(order.getId()) != null) {
                log.error("订单已存在");
                channel.basicAck(deliveryTag, false);
                return;
            }
            orderService.handleOrder(order);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理秒杀订单失败, orderId={}", order == null ? null : order.getId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    //普通订单监听
    @RabbitListener(bindings=@QueueBinding(
            value = @Queue(name = OrderMqConstants.ORDER_BUY_QUEUE,
                    declare = "true",
                    //配置死信交换机和死信路由键
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = OrderMqConstants.ORDER_DEAD_LETTER_EXCHANGE_NAME),
                            @Argument(name = "x-dead-letter-routing-key", value = OrderMqConstants.ORDER_DEAD_LETTER_ROUTING)
                    }
            ),
            exchange = @Exchange(name = OrderMqConstants.ORDER_EXCHANGE_NAME),
            key = OrderMqConstants.ORDER_BUY_ROUTING
    ))
    public void handleBuyOrder(Order order, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("开始处理订单信息: {}", order);
            //判断当前订单是否重复创建
            if (orderService.getById(order.getId()) != null) {
                log.error("订单已存在");
                channel.basicAck(deliveryTag, false);
                return;
            }
            //创建订单
            boolean save = orderService.save(order);
            if (!save) {
                log.error("创建订单失败");
                channel.basicNack(deliveryTag, false, false);
                return;
            }

            // 创建成功，删除 Redis 占位符
            redisService.deleteObject("order:status:" + order.getId());

            //发送延迟消息，检测订单支付状态
            mqMessageSendUtils.sendMqMessage(OrderMqConstants.ORDER_DELAY_EXCHANGE_NAME, OrderMqConstants.ORDER_DELAY_ROUTING, order.getId(), (OrderMqConstants.DELAY_TIME));
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理普通订单失败, orderId={}", order == null ? null : order.getId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    //支付延迟监听
    @RabbitListener(bindings=@QueueBinding(
            value = @Queue(name = OrderMqConstants.ORDER_DELAY_QUEUE),
            exchange = @Exchange(name = OrderMqConstants.ORDER_DELAY_EXCHANGE_NAME,
                    type = "x-delayed-message", // 使用 x-delayed-message 类型交换机
                    durable = "true",
                    arguments = @Argument(name = "x-delayed-type", value = "direct") // 指定路由类型
                    ),
            key = OrderMqConstants.ORDER_DELAY_ROUTING
    ))
    public void handlePayOrder(Long id, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
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
            log.error("处理支付延迟订单失败, orderId={}", id, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
    
    /**
     * 监听死信队列
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = OrderMqConstants.ORDER_DEAD_LETTER_QUEUE, durable = "true"), // 死信队列名
            exchange = @Exchange(value = OrderMqConstants.ORDER_DEAD_LETTER_EXCHANGE_NAME),
            key = OrderMqConstants.ORDER_DEAD_LETTER_ROUTING
    ))
    public void handleDeadLetter(Order order, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.error("死信队列收到订单信息为: {}", order);
        // TODO: 保存到数据库异常表
        channel.basicAck(deliveryTag, false);
    }
}
