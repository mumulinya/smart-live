package com.smartLive.points.listener;

import com.rabbitmq.client.Channel;
import com.smartLive.common.core.constant.RedisMqIdempotentConstants;
import com.smartLive.common.core.constant.mq.PointsMqConstants;
import com.smartLive.common.rabbitmq.domain.OrderPointsMessage;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.points.enums.PointsBizType;
import com.smartLive.points.service.IPointsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 积分模块 MQ 消费者
 * 监听订单核销后的积分奖励消息
 *
 * @author smartLive
 */
@Component
@Slf4j
public class PointsListener {

    @Autowired
    private IPointsService pointsService;

    @Autowired
    private RedisService redisService;

    /**
     * 监听订单核销积分奖励消息
     * 订单核销成功后，根据实付金额给用户增加对应积分
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = PointsMqConstants.POINTS_ORDER_QUEUE, declare = "true"),
            exchange = @Exchange(name = PointsMqConstants.POINTS_DIRECT_EXCHANGE),
            key = PointsMqConstants.POINTS_ORDER_ROUTING_KEY
    ))
    public void handleOrderPoints(OrderPointsMessage msg, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                  @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        if (msg == null || msg.getOrderId() == null || msg.getUserId() == null) {
            log.warn("积分奖励消息为空或缺少必要字段");
            channel.basicAck(deliveryTag, false);
            return;
        }

        if (messageId == null || messageId.isEmpty()) {
            log.error("[MQ幂等] 积分奖励消息缺失 messageId，拒绝消费. orderId={}", msg.getOrderId());
            channel.basicNack(deliveryTag, false, false);
            return;
        }

        String bizKey = "points:order:messageId:" + messageId;
        String idempotentKey = RedisMqIdempotentConstants.POINTS_PREFIX + bizKey;

        try {
            // 幂等校验：防止重复消费
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                log.info("[MQ幂等] 积分奖励重复消息，已跳过，key={}", idempotentKey);
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("[MQ幂等] 首次消费积分奖励消息，key={}", idempotentKey);

            // 计算积分：实付金额（分）转为积分（1分 = 1积分）
            int points = msg.getPayAmount().intValue();
            if (points <= 0) {
                log.warn("积分奖励金额为0，跳过. orderId={}", msg.getOrderId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 调用积分服务增加积分
            pointsService.addPoints(
                    msg.getUserId(),
                    points,
                    PointsBizType.CONSUMPTION.getCode(),
                    String.valueOf(msg.getOrderId()),
                    "订单消费奖励"
            );

            log.info("订单积分奖励成功, orderId={}, userId={}, points={}", msg.getOrderId(), msg.getUserId(), points);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] 积分奖励处理异常，清理幂等锁并触发重试，key={}", idempotentKey, e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("积分奖励处理异常，触发本地重试", e);
        }
    }
}
