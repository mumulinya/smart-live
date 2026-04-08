package com.smartLive.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartLive.common.core.constant.PayTypeConstants;
import com.smartLive.common.core.constant.PaymentStatusConstants;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.order.api.RemoteOrderService;
import com.smartLive.wallet.domain.PaymentRecord;
import com.smartLive.wallet.domain.dto.PaymentBusinessResult;
import com.smartLive.wallet.domain.dto.UnifiedPayDTO;
import com.smartLive.wallet.mapper.PaymentRecordMapper;
import com.smartLive.wallet.service.IWalletService;
import io.seata.spring.annotation.GlobalTransactional;
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
 * 支付 Seata 主事务服务。
 * 仅负责主数据一致性，不负责事务提交后的缓存统计与 MQ 派发。
 */
@Slf4j
@Service
public class WalletPaymentTxService {
    private static final String BIZ_TYPE_RECHARGE = "recharge";
    private static final String BIZ_TYPE_ORDER = "order";

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @Autowired
    private IWalletService walletService;

    @Autowired
    private RemoteOrderService remoteOrderService;

    /**
     * 余额支付主事务。
     * 将支付流水、钱包扣款和订单状态更新纳入同一个 Seata 全局事务。
     *
     * @param userId 当前支付用户
     * @param dto 支付请求参数
     * @return 支付业务处理结果
     */
    @GlobalTransactional(name = "wallet-balance-pay", rollbackFor = Exception.class)
    @Transactional(rollbackFor = Exception.class)
    public PaymentBusinessResult payOrderByBalance(Long userId, UnifiedPayDTO dto) {
        if (!BIZ_TYPE_ORDER.equals(dto.getBizType())) {
            throw new BusinessException("余额支付目前仅支持商品/订单支付");
        }
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("支付金额不合法");
        }

        Long orderId = parseOrderId(dto.getBizId());
        String paySn = generatePaySn();
        Date now = new Date();

        PaymentRecord record = new PaymentRecord();
        record.setPaySn(paySn);
        record.setUserId(userId);
        record.setBizType(dto.getBizType());
        record.setBizId(dto.getBizId());
        record.setAmount(dto.getAmount());
        record.setPayMethod("balance");
        record.setStatus(PaymentStatusConstants.SUCCESS);
        record.setPayTime(now);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        paymentRecordMapper.insert(record);

        walletService.consume(userId, dto.getAmount(), dto.getBizId());

        Integer result = remoteOrderService.paySuccess(orderId, PayTypeConstants.BALANCE);
        if (result == null || result <= 0) {
            throw new BusinessException("支付业务同步失败（订单状态更新异常）");
        }

//        int i = result / 0;// 模拟余额支付后续业务异常，验证 Seata 全局事务回滚
        log.info("余额支付全局事务完成, orderId={}, userId={}", orderId, userId);
        return new PaymentBusinessResult(true, BIZ_TYPE_ORDER, orderId, userId, PayTypeConstants.BALANCE);
    }

    /**
     * 第三方支付成功确认主事务。
     * 统一承接微信回调、支付宝回调、主动查单成功三条路径。
     *
     * @param paySn 支付流水号
     * @param transactionId 第三方交易号
     * @param callbackAmount 回调金额
     * @return 支付业务处理结果
     */
    @GlobalTransactional(name = "wallet-confirm-pay-success", rollbackFor = Exception.class)
    @Transactional(rollbackFor = Exception.class)
    public PaymentBusinessResult confirmPaymentSuccess(String paySn, String transactionId, BigDecimal callbackAmount) {
        PaymentRecord record = findByPaySn(paySn);
        if (record == null) {
            throw new BusinessException("支付记录不存在");
        }

        if (isTerminal(record.getStatus())) {
            log.info("支付记录已处于终态，跳过重复处理, paySn={}, status={}", paySn, record.getStatus());
            return buildNoOpResult(record);
        }

        if (record.getAmount().compareTo(callbackAmount) != 0) {
            throw new BusinessException("金额不一致");
        }

        int updated = updateRecordSuccessIfPending(record.getId(), transactionId);
        if (updated <= 0) {
            PaymentRecord latest = paymentRecordMapper.selectById(record.getId());
            if (latest != null && isTerminal(latest.getStatus())) {
                log.info("支付记录已被其他线程处理，跳过重复分发, paySn={}, status={}", paySn, latest.getStatus());
                return buildNoOpResult(latest);
            }
            throw new BusinessException("支付状态已变更，请稍后重试");
        }

        return dispatchSuccessBusiness(record);
    }

    /**
     * 将支付记录标记为失败。
     * 仅允许待支付记录流转为失败，避免覆盖终态。
     *
     * @param paySn 支付流水号
     * @return 是否更新成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean markPaymentFailed(String paySn) {
        PaymentRecord record = findByPaySn(paySn);
        if (record == null) {
            throw new BusinessException("支付记录不存在");
        }
        if (isTerminal(record.getStatus()) || PaymentStatusConstants.FAILED == record.getStatus()) {
            log.info("支付失败回调命中终态，跳过更新, paySn={}, status={}", paySn, record.getStatus());
            return false;
        }
        return updateRecordFailedIfPending(record.getId()) > 0;
    }

    /**
     * 根据业务类型分发支付成功后的主业务动作。
     *
     * @param record 支付记录
     * @return 支付业务处理结果
     */
    private PaymentBusinessResult dispatchSuccessBusiness(PaymentRecord record) {
        if (BIZ_TYPE_RECHARGE.equals(record.getBizType())) {
            walletService.recharge(record.getUserId(), record.getAmount());
            log.info("充值支付回调处理完成, paySn={}, userId={}", record.getPaySn(), record.getUserId());
            return new PaymentBusinessResult(true, BIZ_TYPE_RECHARGE, null, record.getUserId(), null);
        }

        if (BIZ_TYPE_ORDER.equals(record.getBizType())) {
            walletService.recordOrderPayment(
                    record.getUserId(),
                    record.getAmount(),
                    record.getBizId(),
                    record.getPayMethod()
            );

            int payType = mapPayType(record.getPayMethod());
            Long orderId = parseOrderId(record.getBizId());
            Integer result = remoteOrderService.paySuccess(orderId, payType);
            if (result == null || result <= 0) {
                throw new BusinessException("更新订单状态失败");
            }

            log.info("第三方支付确认分布式事务完成, paySn={}, orderId={}, payType={}", record.getPaySn(), orderId, payType);
            return new PaymentBusinessResult(true, BIZ_TYPE_ORDER, orderId, record.getUserId(), payType);
        }

        throw new BusinessException("不支持的业务类型");
    }

    /**
     * 构造“本次未真正执行业务”的返回结果。
     * 用于重复回调、重复查单等幂等场景。
     *
     * @param record 支付记录
     * @return 业务处理结果
     */
    private PaymentBusinessResult buildNoOpResult(PaymentRecord record) {
        if (record == null) {
            return new PaymentBusinessResult(false, null, null, null, null);
        }
        Long orderId = null;
        if (BIZ_TYPE_ORDER.equals(record.getBizType())) {
            orderId = parseOrderId(record.getBizId());
        }
        return new PaymentBusinessResult(false, record.getBizType(), orderId, record.getUserId(), mapPayType(record.getPayMethod()));
    }

    /**
     * 根据支付流水号查询支付记录。
     *
     * @param paySn 支付流水号
     * @return 支付记录
     */
    private PaymentRecord findByPaySn(String paySn) {
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentRecord::getPaySn, paySn);
        return paymentRecordMapper.selectOne(wrapper);
    }

    /**
     * 条件更新支付记录为成功。
     * 仅允许待支付状态流转为成功，避免重复回调带来的多次执行。
     *
     * @param recordId 支付记录ID
     * @param transactionId 第三方交易号
     * @return 影响行数
     */
    private int updateRecordSuccessIfPending(Long recordId, String transactionId) {
        Date now = new Date();
        LambdaUpdateWrapper<PaymentRecord> update = new LambdaUpdateWrapper<>();
        update.eq(PaymentRecord::getId, recordId)
                .eq(PaymentRecord::getStatus, PaymentStatusConstants.PENDING)
                .set(PaymentRecord::getStatus, PaymentStatusConstants.SUCCESS)
                .set(PaymentRecord::getTransactionId, transactionId)
                .set(PaymentRecord::getPayTime, now)
                .set(PaymentRecord::getUpdateTime, now);
        return paymentRecordMapper.update(null, update);
    }

    /**
     * 条件更新支付记录为失败。
     *
     * @param recordId 支付记录ID
     * @return 影响行数
     */
    private int updateRecordFailedIfPending(Long recordId) {
        Date now = new Date();
        LambdaUpdateWrapper<PaymentRecord> update = new LambdaUpdateWrapper<>();
        update.eq(PaymentRecord::getId, recordId)
                .eq(PaymentRecord::getStatus, PaymentStatusConstants.PENDING)
                .set(PaymentRecord::getStatus, PaymentStatusConstants.FAILED)
                .set(PaymentRecord::getUpdateTime, now);
        return paymentRecordMapper.update(null, update);
    }

    /**
     * 判断支付记录是否已处于终态。
     *
     * @param status 支付状态
     * @return true=终态
     */
    private boolean isTerminal(Integer status) {
        return PaymentStatusConstants.SUCCESS == status
                || PaymentStatusConstants.CANCELED == status
                || PaymentStatusConstants.FAILED == status;
    }

    /**
     * 将支付方式字符串映射为系统内支付类型常量。
     *
     * @param payMethod 支付方式
     * @return 支付类型
     */
    private int mapPayType(String payMethod) {
        if ("wechat".equals(payMethod)) {
            return PayTypeConstants.WECHAT;
        }
        if ("balance".equals(payMethod)) {
            return PayTypeConstants.BALANCE;
        }
        return PayTypeConstants.ALIPAY;
    }

    /**
     * 解析订单业务ID。
     *
     * @param bizId 业务ID
     * @return 订单ID
     */
    private Long parseOrderId(String bizId) {
        try {
            return Long.parseLong(bizId);
        } catch (NumberFormatException e) {
            throw new BusinessException("系统业务单号格式非法");
        }
    }

    /**
     * 生成余额支付使用的支付流水号。
     *
     * @return 支付流水号
     */
    private String generatePaySn() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "P" + date + random;
    }
}
