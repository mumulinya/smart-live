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
 * 站内余额支付专用服务
 * 专门处理通过用户账户可用余额抵扣订单金额的逻辑，包含余额预扣、动账记录生成及订单状态异步通知。
 *
 * @author smartLive
 * @date 2026-03-11
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
     * 执行余额支付逻辑
     * 1. 落地本地支付单据记录 (状态直接为成功)
     * 2. 调用钱包服务执行原子扣减并记录账单流水
     * 3. 同步调用订单模块标记支付成功
     * 
     * @param userId 支付用户 ID
     * @param dto 支付请求参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long userId, UnifiedPayDTO dto) {
        if (!"order".equals(dto.getBizType())) {
            throw new BusinessException("余额支付目前仅支持商品/订单支付");
        }
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("支付金额不合法");
        }

        // 1. 生成支付流水号
        String paySn = generatePaySn();

        // 2. 创建支付流水记录
        PaymentRecord record = new PaymentRecord();
        record.setPaySn(paySn);
        record.setUserId(userId);
        record.setBizType(dto.getBizType());
        record.setBizId(dto.getBizId());
        record.setAmount(dto.getAmount());
        record.setPayMethod("balance");
        record.setStatus(1); // 余额扣减成功即视为支付成功
        record.setPayTime(new Date());
        record.setCreateTime(new Date());
        record.setUpdateTime(new Date());
        paymentRecordMapper.insert(record);

        // 3. 执行资产扣减 + 动账存证
        try {
            BigDecimal newBalance = walletService.consume(userId, dto.getAmount(), dto.getBizId());
            log.info("余额扣除成功: userId={}, amount={}, 剩余余额={}", userId, dto.getAmount(), newBalance);
        } catch (Exception e) {
            log.error("余额扣除操作异常: userId={}, amount={}", userId, dto.getAmount(), e);
            throw new BusinessException("账户余额不足或扣费异常");
        }

        // 4. Feign RPC 同步修改订单状态
        try {
            Long orderId = Long.parseLong(dto.getBizId());
            Integer result = remoteOrderService.paySuccess(orderId, PayTypeConstants.BALANCE);
            if (result == null || result <= 0) {
                log.error("RPC 标记订单支付成功失败, orderId={}", orderId);
                throw new BusinessException("支付业务同步失败（订单状态更新异常）");
            }
            log.info("订单业务支付状态更新完成, orderId={}", orderId);
        } catch (NumberFormatException e) {
            log.error("业务单号解析错误, bizId={}", dto.getBizId(), e);
            throw new BusinessException("系统业务单号格式非法");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("订单状态同步流程异常", e);
            throw new BusinessException("支付环节系统繁忙，请确认订单状态");
        }
    }

    private String generatePaySn() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "P" + date + random;
    }
}
