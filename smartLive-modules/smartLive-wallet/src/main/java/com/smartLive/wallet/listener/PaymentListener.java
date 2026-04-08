package com.smartLive.wallet.listener;
import com.smartLive.common.core.constant.PaymentStatusConstants;
import com.smartLive.common.core.constant.mq.OrderMqConstants;
import com.smartLive.common.core.constant.RedisMqIdempotentConstants;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rabbitmq.client.Channel;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.wallet.domain.PaymentRecord;
import com.smartLive.wallet.mapper.PaymentRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 支付超时延迟消息监听器
 * 当延迟消息到达后，检查支付记录状态，如果仍然是待支付则自动取消
 *
 * @author smartLive
 */
@Component
@Slf4j
public class PaymentListener {

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @Autowired
    private RedisService redisService;

    /**
     * 监听支付超时延迟消息
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = OrderMqConstants.PAY_DELAY_QUEUE,
                    arguments = {
                            @Argument(name = "x-dead-letter-exchange", value = OrderMqConstants.PAY_DLX_EXCHANGE),
                            @Argument(name = "x-dead-letter-routing-key", value = OrderMqConstants.PAY_DLQ_ROUTING_KEY)
                    }
            ),
            exchange = @Exchange(name = OrderMqConstants.PAY_DELAY_EXCHANGE,
                    type = "x-delayed-message",
                    durable = "true",
                    arguments = @Argument(name = "x-delayed-type", value = "direct")
            ),
            key = OrderMqConstants.PAY_DELAY_ROUTING_KEY
    ))
    public void handlePayTimeout(Long recordId, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, @Header(required = false, name = AmqpHeaders.MESSAGE_ID) String messageId) throws IOException {
        if (messageId == null || messageId.isEmpty()) {
            log.error("[MQ幂等] 支付超时延迟消息缺失 messageId，拒绝消费. recordId={}", recordId);
            channel.basicNack(deliveryTag, false, false);
            return;
        }
        
        String bizKey = "messageId:" + messageId;
        String idempotentKey = RedisMqIdempotentConstants.PAY_PREFIX + bizKey;

        try {
            if (!redisService.tryConsumeOnce(idempotentKey, RedisMqIdempotentConstants.DEFAULT_TTL_SECONDS)) {
                log.info("[MQ幂等] 重复消息，已跳过，key={}", idempotentKey);
                channel.basicAck(deliveryTag, false);
                return;
            }
            log.info("[MQ幂等] 首次消费，key={}", idempotentKey);

            PaymentRecord record = paymentRecordMapper.selectById(recordId);
            if (record == null) {
                log.warn("支付记录不存在, recordId={}", recordId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 只有待支付状态才需要取消
            if (record.getStatus() != null && record.getStatus() == PaymentStatusConstants.PENDING) {
                log.info("支付超时，自动取消, paySn={}, recordId={}", record.getPaySn(), recordId);

                LambdaUpdateWrapper<PaymentRecord> update = new LambdaUpdateWrapper<>();
                update.eq(PaymentRecord::getId, recordId)
                        .eq(PaymentRecord::getStatus, PaymentStatusConstants.PENDING) // 乐观锁: 确保还是待支付
                        .set(PaymentRecord::getStatus, PaymentStatusConstants.CANCELED)
                        .set(PaymentRecord::getUpdateTime, LocalDateTime.now());
                int rows = paymentRecordMapper.update(null, update);

                if (rows > 0) {
                    log.info("支付记录已自动取消, paySn={}", record.getPaySn());
                } else {
                    log.info("支付记录状态已变更(无需取消), paySn={}", record.getPaySn());
                }
            } else {
                log.info("支付记录非待支付状态，无需处理, paySn={}, status={}", record.getPaySn(), record.getStatus());
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ幂等] 支付超时处理异常，清理幂等锁并触发重试，key={}", idempotentKey, e);
            redisService.deleteObject(idempotentKey);
            throw new RuntimeException("支付超时处理异常，触发本地重试", e);
        }
    }

    /**
     * 监听支付处理死信队列
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = OrderMqConstants.PAY_DLQ_QUEUE, durable = "true"),
            exchange = @Exchange(value = OrderMqConstants.PAY_DLX_EXCHANGE),
            key = OrderMqConstants.PAY_DLQ_ROUTING_KEY
    ))
    public void handlePayDeadLetter(org.springframework.amqp.core.Message message) {
        log.error("pay dead letter received: {}", new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8));
    }
}
