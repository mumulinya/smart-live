package com.smartLive.wallet.service;

import com.smartLive.wallet.domain.vo.WalletInfoVO;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Wallet service.
 */
public interface IWalletService {

    /**
     * Query wallet info.
     */
    WalletInfoVO getWalletInfo(Long userId);

    /**
     * Query wallet transaction page.
     */
    Map<String, Object> getTransactionList(Long userId, Integer page, Integer pageSize, String type);

    /**
     * Mock recharge.
     */
    BigDecimal recharge(Long userId, BigDecimal amount);

    /**
     * Set or update pay password.
     */
    void setPayPassword(Long userId, String password);

    /**
     * Verify pay password.
     */
    boolean verifyPayPassword(Long userId, String password);

    /**
     * Admin adjusts user balance.
     */
    BigDecimal adjustBalance(Long userId, BigDecimal amount, Integer type, String remark);

    /**
     * 记录订单支付流水（不影响余额）
     *
     * @param userId    用户ID
     * @param amount    支付金额
     * @param bizId     业务ID（订单ID）
     * @param payMethod 支付方式
     */
    void recordOrderPayment(Long userId, BigDecimal amount, String bizId, String payMethod);

    /**
     * 余额消费（扣减余额 + 记录消费流水）
     *
     * @param userId 用户ID
     * @param amount 消费金额
     * @param bizId  业务ID（订单ID）
     * @return 扣减后的余额
     */
    BigDecimal consume(Long userId, BigDecimal amount, String bizId);

    /**
     * 订单退款恢复余额
     *
     * @param userId       用户ID
     * @param amountInCents 退款金额（分）
     * @param orderId      订单ID
     */
    void refundOrder(Long userId, BigDecimal amountInCents, String orderId);
}
