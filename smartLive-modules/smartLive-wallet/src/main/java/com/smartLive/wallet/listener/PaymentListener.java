package com.smartLive.wallet.listener;
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
                            @Argument(name = "x-dead-letter-exchange", value = OrderMqConstants.PAY_DEAD_LETTER_EXCHANGE_NAME),
                            @Argument(name = "x-dead-letter-routing-key", value = OrderMqConstants.PAY_DEAD_LETTER_ROUTING)
                    }
            ),
            exchange = @Exchange(name = OrderMqConstants.PAY_DELAY_EXCHANGE_NAME,
                    type = "x-delayed-message",
                    durable = "true",
                    arguments = @Argument(name = "x-delayed-type", value = "direct")
            ),
            key = OrderMqConstants.PAY_DELAY_ROUTING
    ))
    public void handlePayTimeout(Long recordId, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        String idempotentKey = RedisMqIdempotentConstants.PAY_PREFIX + recordId;

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

            // 只有待支付状态(0)才需要取消
            if (record.getStatus() != null && record.getStatus() == 0) {
                log.info("支付超时，自动取消, paySn={}, recordId={}", record.getPaySn(), recordId);

                LambdaUpdateWrapper<PaymentRecord> update = new LambdaUpdateWrapper<>();
                update.eq(PaymentRecord::getId, recordId)
                        .eq(PaymentRecord::getStatus, 0) // 乐观锁: 确保还是待支付
                        .set(PaymentRecord::getStatus, 3) // 3: 已取消/已过期
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
            log.error("[MQ幂等] 支付超时处理失败，key={}", idempotentKey, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = OrderMqConstants.PAY_DEAD_LETTER_QUEUE, durable = "true"),
            exchange = @Exchange(value = OrderMqConstants.PAY_DEAD_LETTER_EXCHANGE_NAME),
            key = OrderMqConstants.PAY_DEAD_LETTER_ROUTING
    ))
    public void handlePayDeadLetter(org.springframework.amqp.core.Message message) {
        log.error("pay dead letter received: {}", new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8));
    }
}
