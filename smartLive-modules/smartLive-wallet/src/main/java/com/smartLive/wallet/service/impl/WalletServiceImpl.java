package com.smartLive.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.common.core.utils.StringUtils;
import com.smartLive.common.security.utils.SecurityUtils;
import com.smartLive.wallet.domain.UserWallet;
import com.smartLive.wallet.domain.WalletTransaction;
import com.smartLive.wallet.domain.vo.WalletInfoVO;
import com.smartLive.wallet.domain.vo.WalletTransactionVO;
import com.smartLive.wallet.mapper.UserWalletMapper;
import com.smartLive.wallet.mapper.WalletTransactionMapper;
import com.smartLive.wallet.service.IWalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Wallet service implementation.
 */
@Slf4j
@Service
public class WalletServiceImpl implements IWalletService {

    private static final int WALLET_STATUS_NORMAL = 1;

    private static final int DIRECTION_IN = 1;
    private static final int DIRECTION_OUT = 2;

    private static final int TRANSACTION_TYPE_RECHARGE = 1;
    private static final int TRANSACTION_TYPE_CONSUME = 3;

    private static final int TRANSACTION_STATUS_PENDING = 0;
    private static final int TRANSACTION_STATUS_SUCCESS = 1;
    private static final int TRANSACTION_STATUS_FAILED = 2;

    private static final String BIZ_TYPE_RECHARGE = "recharge";
    private static final String BIZ_TYPE_ADMIN_ADJUST = "admin_adjust";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private UserWalletMapper userWalletMapper;

    @Autowired
    private WalletTransactionMapper walletTransactionMapper;

    @Override
    public WalletInfoVO getWalletInfo(Long userId) {
        UserWallet wallet = getOrCreateWallet(userId);
        WalletInfoVO info = new WalletInfoVO();
        info.setBalance(wallet.getBalance());
        info.setFrozenBalance(wallet.getFrozenBalance());
        info.setHasPayPassword(StringUtils.isNotEmpty(wallet.getPayPassword()));
        return info;
    }

    @Override
    public Map<String, Object> getTransactionList(Long userId, Integer page, Integer pageSize, String type) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        String queryType = StringUtils.isEmpty(type) ? "all" : type;

        LambdaQueryWrapper<WalletTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WalletTransaction::getUserId, userId);
        if ("in".equalsIgnoreCase(queryType)) {
            wrapper.eq(WalletTransaction::getDirection, DIRECTION_IN);
        } else if ("out".equalsIgnoreCase(queryType)) {
            wrapper.eq(WalletTransaction::getDirection, DIRECTION_OUT);
        }
        wrapper.orderByDesc(WalletTransaction::getCreateTime);

        Page<WalletTransaction> pageResult = walletTransactionMapper.selectPage(new Page<>(pageNo, size), wrapper);

        List<WalletTransactionVO> records = new ArrayList<>(pageResult.getRecords().size());
        for (WalletTransaction transaction : pageResult.getRecords()) {
            WalletTransactionVO item = new WalletTransactionVO();
            item.setId(transaction.getId());
            item.setTitle(transaction.getTitle());
            item.setTime(transaction.getCreateTime() == null ? null : transaction.getCreateTime().toString());
            item.setAmount(transaction.getAmount());
            item.setType(transaction.getDirection() != null && transaction.getDirection() == DIRECTION_IN ? "in" : "out");
            item.setStatus(toStatusText(transaction.getStatus()));
            records.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", pageResult.getTotal());
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal recharge(Long userId, BigDecimal amount) {
        BigDecimal rechargeAmount = normalizeAmount(amount);
        UserWallet wallet = getOrCreateWallet(userId);
        ensureWalletUsable(wallet);

        BigDecimal newBalance = addBalance(userId, rechargeAmount);
        insertTransaction(
                userId,
                TRANSACTION_TYPE_RECHARGE,
                rechargeAmount,
                DIRECTION_IN,
                newBalance,
                BIZ_TYPE_RECHARGE,
                null,
                "Balance recharge",
                TRANSACTION_STATUS_SUCCESS,
                "Mock recharge"
        );
        return newBalance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setPayPassword(Long userId, String password) {
        if (StringUtils.isEmpty(password) || password.length() < 6) {
            throw new BusinessException("Password must be at least 6 characters");
        }

        UserWallet wallet = getOrCreateWallet(userId);
        ensureWalletUsable(wallet);
        wallet.setPayPassword(SecurityUtils.encryptPassword(password));
        wallet.setUpdateTime(new Date());
        if (userWalletMapper.updateById(wallet) <= 0) {
            throw new BusinessException("Failed to set pay password");
        }
    }

    @Override
    public boolean verifyPayPassword(Long userId, String password) {
        if (StringUtils.isEmpty(password)) {
            return false;
        }
        UserWallet wallet = getOrCreateWallet(userId);
        if (!isWalletUsable(wallet)) {
            return false;
        }
        if (StringUtils.isEmpty(wallet.getPayPassword())) {
            return false;
        }
        return SecurityUtils.matchesPassword(password, wallet.getPayPassword());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal adjustBalance(Long userId, BigDecimal amount, Integer type, String remark) {
        BigDecimal adjustAmount = normalizeAmount(amount);
        getOrCreateWallet(userId);

        if (type == null || (type != 1 && type != 2)) {
            throw new BusinessException("Unsupported adjust type");
        }

        if (type == 1) {
            BigDecimal newBalance = addBalance(userId, adjustAmount);
            insertTransaction(
                    userId,
                    TRANSACTION_TYPE_RECHARGE,
                    adjustAmount,
                    DIRECTION_IN,
                    newBalance,
                    BIZ_TYPE_ADMIN_ADJUST,
                    null,
                    "Admin add balance",
                    TRANSACTION_STATUS_SUCCESS,
                    remark
            );
            return newBalance;
        }

        BigDecimal newBalance = deductBalance(userId, adjustAmount);
        insertTransaction(
                userId,
                TRANSACTION_TYPE_CONSUME,
                adjustAmount,
                DIRECTION_OUT,
                newBalance,
                BIZ_TYPE_ADMIN_ADJUST,
                null,
                "Admin deduct balance",
                TRANSACTION_STATUS_SUCCESS,
                remark
        );
        return newBalance;
    }

    private UserWallet getOrCreateWallet(Long userId) {
        if (userId == null) {
            throw new BusinessException("User id cannot be null");
        }

        UserWallet wallet = userWalletMapper.selectById(userId);
        if (wallet != null) {
            return wallet;
        }

        UserWallet initWallet = new UserWallet();
        initWallet.setUserId(userId);
        initWallet.setBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        initWallet.setFrozenBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        initWallet.setStatus(WALLET_STATUS_NORMAL);
        initWallet.setVersion(0);
        initWallet.setUpdateTime(new Date());
        try {
            userWalletMapper.insert(initWallet);
            return initWallet;
        } catch (DuplicateKeyException ex) {
            log.warn("Wallet was created concurrently, userId={}", userId);
            UserWallet latest = userWalletMapper.selectById(userId);
            if (latest == null) {
                throw new BusinessException("Failed to initialize wallet");
            }
            return latest;
        }
    }

    private BigDecimal addBalance(Long userId, BigDecimal amount) {
        LambdaUpdateWrapper<UserWallet> update = new LambdaUpdateWrapper<>();
        update.eq(UserWallet::getUserId, userId)
                .setSql("balance = balance + " + amount.toPlainString())
                .setSql("version = version + 1")
                .set(UserWallet::getUpdateTime, LocalDateTime.now());
        if (userWalletMapper.update(null, update) <= 0) {
            throw new BusinessException("Balance update failed");
        }
        UserWallet latest = userWalletMapper.selectById(userId);
        return latest.getBalance();
    }

    private BigDecimal deductBalance(Long userId, BigDecimal amount) {
        LambdaUpdateWrapper<UserWallet> update = new LambdaUpdateWrapper<>();
        update.eq(UserWallet::getUserId, userId)
                .ge(UserWallet::getBalance, amount)
                .setSql("balance = balance - " + amount.toPlainString())
                .setSql("version = version + 1")
                .set(UserWallet::getUpdateTime, LocalDateTime.now());
        if (userWalletMapper.update(null, update) <= 0) {
            throw new BusinessException("Insufficient balance");
        }
        UserWallet latest = userWalletMapper.selectById(userId);
        return latest.getBalance();
    }

    @Override
    public void recordOrderPayment(Long userId, BigDecimal amount, String bizId, String payMethod) {
        UserWallet wallet = getOrCreateWallet(userId);
        BigDecimal currentBalance = wallet.getBalance();

        String title;
        if ("alipay".equals(payMethod)) {
            title = "支付宝支付订单";
        } else if ("wechat".equals(payMethod)) {
            title = "微信支付订单";
        } else if ("balance".equals(payMethod)) {
            title = "余额支付订单";
        } else {
            title = "支付订单";
        }

        insertTransaction(
                userId,
                TRANSACTION_TYPE_CONSUME,
                normalizeAmount(amount),
                DIRECTION_OUT,
                currentBalance, // 第三方支付不影响余额
                "order",
                bizId,
                title,
                TRANSACTION_STATUS_SUCCESS,
                "订单支付 - " + payMethod
        );
        log.info("记录订单支付流水, userId={}, amount={}, bizId={}, payMethod={}", userId, amount, bizId, payMethod);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal consume(Long userId, BigDecimal amount, String bizId) {
        BigDecimal consumeAmount = normalizeAmount(amount);
        UserWallet wallet = getOrCreateWallet(userId);
        ensureWalletUsable(wallet);

        if (wallet.getBalance().compareTo(consumeAmount) < 0) {
            throw new BusinessException("余额不足");
        }

        BigDecimal newBalance = deductBalance(userId, consumeAmount);
        insertTransaction(
                userId,
                TRANSACTION_TYPE_CONSUME,
                consumeAmount,
                DIRECTION_OUT,
                newBalance,
                "order",
                bizId,
                "余额支付订单",
                TRANSACTION_STATUS_SUCCESS,
                "余额支付"
        );
        return newBalance;
    }

    private void insertTransaction(Long userId,
                                   Integer type,
                                   BigDecimal amount,
                                   Integer direction,
                                   BigDecimal balanceAfter,
                                   String bizType,
                                   String bizId,
                                   String title,
                                   Integer status,
                                   String remark) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUserId(userId);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setDirection(direction);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setBizType(bizType);
        transaction.setBizId(bizId);
        transaction.setTitle(title);
        transaction.setStatus(status);
        transaction.setRemark(remark);
        transaction.setCreateTime(new Date());
        transaction.setUpdateTime(new Date());
        walletTransactionMapper.insert(transaction);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BusinessException("Amount cannot be null");
        }
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be greater than 0");
        }
        return normalized;
    }

    private void ensureWalletUsable(UserWallet wallet) {
        if (!isWalletUsable(wallet)) {
            throw new BusinessException("Wallet is frozen");
        }
    }

    private boolean isWalletUsable(UserWallet wallet) {
        return wallet != null && (wallet.getStatus() == null || wallet.getStatus() == WALLET_STATUS_NORMAL);
    }

    private String toStatusText(Integer status) {
        if (status == null) {
            return "failed";
        }
        if (status == TRANSACTION_STATUS_SUCCESS) {
            return "success";
        }
        if (status == TRANSACTION_STATUS_PENDING) {
            return "pending";
        }
        if (status == TRANSACTION_STATUS_FAILED) {
            return "failed";
        }
        return "failed";
    }
}
