package com.smartLive.wallet.service.impl;

import com.smartLive.common.core.constant.PayTypeConstants;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.order.api.RemoteOrderService;
import com.smartLive.wallet.domain.PaymentRecord;
import com.smartLive.wallet.domain.dto.UnifiedPayDTO;
import com.smartLive.wallet.mapper.PaymentRecordMapper;
import com.smartLive.wallet.service.IWalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

/**
 * 余额支付服务
 *
 * @author smartLive
 */
@Slf4j
@Service
public class BalancePayService {

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @Autowired
    private IWalletService walletService;

    @Autowired
    private RemoteOrderService remoteOrderService;

    /**
     * 余额支付
     */
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long userId, UnifiedPayDTO dto) {
        if (!"order".equals(dto.getBizType())) {
            throw new BusinessException("余额支付仅支持订单支付");
        }
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("支付金额异常");
        }

        // 1. 生成支付流水号
        String paySn = generatePaySn();

        // 2. 创建支付记录
        PaymentRecord record = new PaymentRecord();
        record.setPaySn(paySn);
        record.setUserId(userId);
        record.setBizType(dto.getBizType());
        record.setBizId(dto.getBizId());
        record.setAmount(dto.getAmount());
        record.setPayMethod("balance");
        record.setStatus(1); // 直接成功
        record.setPayTime(new Date());
        record.setCreateTime(new Date());
        record.setUpdateTime(new Date());
        paymentRecordMapper.insert(record);

        // 3. 扣减余额 + 记录消费流水
        try {
            BigDecimal newBalance = walletService.consume(userId, dto.getAmount(), dto.getBizId());
            log.info("余额扣减成功: userId={}, amount={}, newBalance={}", userId, dto.getAmount(), newBalance);
        } catch (Exception e) {
            log.error("余额扣减失败: userId={}, amount={}", userId, dto.getAmount(), e);
            throw new BusinessException("余额不足或扣减失败");
        }

        // 4. Feign调用订单模块更新订单状态
        try {
            Long orderId = Long.parseLong(dto.getBizId());
            Integer result = remoteOrderService.paySuccess(orderId, PayTypeConstants.BALANCE);
            if (result == null || result <= 0) {
                log.error("更新订单状态失败, orderId={}", orderId);
                throw new BusinessException("更新订单状态失败");
            }
            log.info("订单支付成功, orderId={}", orderId);
        } catch (NumberFormatException e) {
            log.error("订单ID格式错误, bizId={}", dto.getBizId(), e);
            throw new BusinessException("订单ID格式错误");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新订单状态失败", e);
            throw new BusinessException("支付失败，请稍后重试");
        }
    }

    private String generatePaySn() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "P" + date + random;
    }
}
