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
 * 钱包与账务明细业务处理实现类
 * 处理核心余额账户的日常操作（增减余额、乐观锁防超卖）、交易流水审计记录以及安全支付密码核验。
 * 
 * @author smartLive
 * @date 2026-03-11
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

    /**
     * 余额充值处理
     * 使用数据库原子自增防止余额被并发覆盖。
     */
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
                "余额充值",
                TRANSACTION_STATUS_SUCCESS,
                "模拟成功确认"
        );
        return newBalance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setPayPassword(Long userId, String password) {
        if (StringUtils.isEmpty(password) || password.length() < 6) {
            throw new BusinessException("支付密码至少需要6位");
        }

        UserWallet wallet = getOrCreateWallet(userId);
        ensureWalletUsable(wallet);
        wallet.setPayPassword(SecurityUtils.encryptPassword(password));
        wallet.setUpdateTime(new Date());
        if (userWalletMapper.updateById(wallet) <= 0) {
            throw new BusinessException("设置支付密码失败");
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
            throw new BusinessException("不支持的调整类型");
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
                    "后台手工增额",
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
                "后台手工扣额",
                TRANSACTION_STATUS_SUCCESS,
                remark
            );
        return newBalance;
    }

    /**
     * 获取或初始化用户钱包
     * 针对新注册或首次使用的用户，通过数据库唯一约束安全地创建钱包记录。
     */
    private UserWallet getOrCreateWallet(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户标识不能为空");
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
            log.warn("钱包记录已被其他线程初始化, userId={}", userId);
            UserWallet latest = userWalletMapper.selectById(userId);
            if (latest == null) {
                throw new BusinessException("钱包初始化异常");
            }
            return latest;
        }
    }

    /**
     * 原子增加余额
     * 避免在高并发下读取 -> 计算 -> 写入导致的数据不一致问题。
     */
    private BigDecimal addBalance(Long userId, BigDecimal amount) {
        LambdaUpdateWrapper<UserWallet> update = new LambdaUpdateWrapper<>();
        update.eq(UserWallet::getUserId, userId)
                .setSql("balance = balance + " + amount.toPlainString())
                .setSql("version = version + 1")
                .set(UserWallet::getUpdateTime, LocalDateTime.now());
        if (userWalletMapper.update(null, update) <= 0) {
            throw new BusinessException("余额入账更新失败");
        }
        UserWallet latest = userWalletMapper.selectById(userId);
        return latest.getBalance();
    }

    /**
     * 原子减少余额并校验足够扣减
     * 本质上通过 SQL 条件 `balance >= amount` 保证不超扣。
     */
    private BigDecimal deductBalance(Long userId, BigDecimal amount) {
        LambdaUpdateWrapper<UserWallet> update = new LambdaUpdateWrapper<>();
        update.eq(UserWallet::getUserId, userId)
                .ge(UserWallet::getBalance, amount)
                .setSql("balance = balance - " + amount.toPlainString())
                .setSql("version = version + 1")
                .set(UserWallet::getUpdateTime, LocalDateTime.now());
        if (userWalletMapper.update(null, update) <= 0) {
            throw new BusinessException("钱包余额不足");
        }
        UserWallet latest = userWalletMapper.selectById(userId);
        return latest.getBalance();
    }

    /**
     * 记录订单支付的流水（非余额支付渠道）
     * 微信/支付宝支付时，余额不发生变化，但需记录账务流水。
     */
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
                currentBalance, // 外部支付不影响本地余额
                "order",
                bizId,
                title,
                TRANSACTION_STATUS_SUCCESS,
                "在线支付渠道 - " + payMethod
        );
        log.info("审计：记录订单外部支付流水, userId={}, amount={}, bizId={}, payMethod={}", userId, amount, bizId, payMethod);
    }

    /**
     * 站内余额消费接口
     * 扣减余额并记录动账记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal consume(Long userId, BigDecimal amount, String bizId) {
        BigDecimal consumeAmount = normalizeAmount(amount);
        UserWallet wallet = getOrCreateWallet(userId);
        ensureWalletUsable(wallet);

        if (wallet.getBalance().compareTo(consumeAmount) < 0) {
            throw new BusinessException("账户余额不足");
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
                "站内余额全额抵扣"
        );
        return newBalance;
    }

    /**
     * 订单退款（原路退回余额）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(Long userId, BigDecimal refundAmount, String orderId) {
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("退款金额无效, userId={}, orderId={}, amount={}", userId, orderId, refundAmount);
            return;
        }
        BigDecimal newBalance = addBalance(userId, refundAmount);
        insertTransaction(
                userId,
                TRANSACTION_TYPE_RECHARGE,
                refundAmount,
                DIRECTION_IN,
                newBalance,
                "refund",
                orderId,
                "订单支付退款",
                TRANSACTION_STATUS_SUCCESS,
                "全额或部分退款至原账户"
        );
        log.info("订单已退款入账, userId={}, orderId={}, amount={}, newBalance={}", userId, orderId, refundAmount, newBalance);
    }

    /**
     * 插入统一动账流水记录
     */
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
            throw new BusinessException("金额不能为空");
        }
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("操作金额必须大于 0");
        }
        return normalized;
    }

    private void ensureWalletUsable(UserWallet wallet) {
        if (!isWalletUsable(wallet)) {
            throw new BusinessException("钱包账户已被冻结或无法使用");
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
