package com.smartLive.wallet.listener;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.wallet.domain.PaymentRecord;
import com.smartLive.wallet.mapper.PaymentRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    /**
     * 监听支付超时延迟消息
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstants.PAY_DELAY_QUEUE),
            exchange = @Exchange(name = MqConstants.PAY_DELAY_EXCHANGE_NAME,
                    type = "x-delayed-message",
                    durable = "true",
                    arguments = @Argument(name = "x-delayed-type", value = "direct")
            ),
            key = MqConstants.PAY_DELAY_ROUTING
    ))
    public void handlePayTimeout(Long recordId) {
        log.info("收到支付超时延迟消息, recordId={}", recordId);

        PaymentRecord record = paymentRecordMapper.selectById(recordId);
        if (record == null) {
            log.warn("支付记录不存在, recordId={}", recordId);
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
    }
}
