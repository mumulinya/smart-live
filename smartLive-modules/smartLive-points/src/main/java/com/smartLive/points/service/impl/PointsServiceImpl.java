package com.smartLive.points.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.points.domain.DailySignIn;
import com.smartLive.points.domain.PointsRecord;
import com.smartLive.points.domain.UserPointsWallet;
import com.smartLive.points.domain.vo.*;
import com.smartLive.points.mapper.DailySignInMapper;
import com.smartLive.points.mapper.PointsLotteryPrizeMapper;
import com.smartLive.points.mapper.PointsRecordMapper;
import com.smartLive.points.mapper.UserPointsWalletMapper;
import com.smartLive.points.service.IPointsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 积分服务实现
 *
 * @author smartLive
 */
@Slf4j
@Service
public class PointsServiceImpl implements IPointsService {

    @Autowired
    private UserPointsWalletMapper walletMapper;

    @Autowired
    private PointsRecordMapper recordMapper;

    @Autowired
    private DailySignInMapper signInMapper;

    @Autowired
    private PointsLotteryPrizeMapper prizeMapper;

    /** 每次抽奖消耗积分 */
    private static final int LOTTERY_COST = 50;

    /** 签到基础积分 */
    private static final int SIGN_IN_BASE_POINTS = 10;

    // ==================== 等级配置 ====================

    /**
     * 等级阈值定义
     * 普通 < 1000 < 银卡 < 3000 < 金卡 < 8000 < 钻石 < 20000 < 至尊
     */
    private static final int[][] LEVEL_THRESHOLDS = {
            {0, 1000},      // 普通
            {1000, 3000},   // 银卡
            {3000, 8000},   // 金卡
            {8000, 20000},  // 钻石
            {20000, Integer.MAX_VALUE} // 至尊
    };

    private static final String[] LEVEL_NAMES = {"普通", "银卡", "金卡", "钻石", "至尊"};

    /** 各等级权益 */
    private static final String[][][] LEVEL_BENEFITS = {
            {}, // 普通无权益
            {{"9.8折券 x1", "本月可用"}},
            {{"95折券 x3", "本月可用"}, {"免邮额度 +20元", "满减叠加"}},
            {{"9折券 x3", "本月可用"}, {"免邮额度 +50元", "满减叠加"}, {"专属客服", "优先响应"}},
            {{"85折券 x5", "本月可用"}, {"免邮额度 +100元", "满减叠加"}, {"专属客服", "优先响应"}, {"生日礼包", "每年一次"}}
    };


    // ==================== 接口实现 ====================

    @Override
    public WalletInfoVO getWalletInfo(Long userId) {
        // 1. 获取或初始化钱包
        UserPointsWallet wallet = getOrCreateWallet(userId);

        // 2. 计算等级信息
        int totalEarned = wallet.getTotalEarned();
        int levelIndex = calcLevelIndex(totalEarned);

        WalletInfoVO vo = new WalletInfoVO();
        vo.setBalance(wallet.getBalance());
        vo.setTotalEarned(totalEarned);
        vo.setLevelName(LEVEL_NAMES[levelIndex]);
        vo.setConsecutiveDays(wallet.getConsecutiveDays());

        // 计算进度
        if (levelIndex < LEVEL_THRESHOLDS.length - 1) {
            int currentMin = LEVEL_THRESHOLDS[levelIndex][0];
            int currentMax = LEVEL_THRESHOLDS[levelIndex][1];
            int range = currentMax - currentMin;
            int progress = totalEarned - currentMin;
            vo.setProgressPercent(Math.min(100, (int) ((double) progress / range * 100)));
            vo.setNextLevelNeed(currentMax - totalEarned);
        } else {
            // 最高等级
            vo.setProgressPercent(100);
            vo.setNextLevelNeed(0);
        }

        // 3. 判断今日是否已签到
        LocalDate today = LocalDate.now();
        vo.setSignedIn(today.equals(wallet.getLastSignDate()));

        // 4. 设置权益
        List<WalletInfoVO.BenefitItem> benefits = new ArrayList<>();
        String[][] levelBenefits = LEVEL_BENEFITS[levelIndex];
        for (String[] b : levelBenefits) {
            WalletInfoVO.BenefitItem item = new WalletInfoVO.BenefitItem();
            item.setTitle(b[0]);
            item.setDesc(b[1]);
            benefits.add(item);
        }
        vo.setBenefits(benefits);

        return vo;
    }

    @Override
    public List<PointsRecordVO> getRecordList(Long userId, Integer page, Integer pageSize, Integer type) {
        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;

        LambdaQueryWrapper<PointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsRecord::getUserId, userId);

        // 类型筛选
        wrapper.eq(type != null,PointsRecord::getType, type);

        wrapper.orderByDesc(PointsRecord::getCreateTime);

        Page<PointsRecord> pageObj = new Page<>(page, pageSize);
        Page<PointsRecord> result = recordMapper.selectPage(pageObj, wrapper);

        // 转换VO
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<PointsRecordVO> records = new ArrayList<>();
        for (PointsRecord r : result.getRecords()) {
            PointsRecordVO vo = new PointsRecordVO();
            vo.setId(r.getId());
            vo.setDate(r.getCreateTime() != null ? r.getCreateTime().format(fmt) : "");
            vo.setDesc(r.getDescription());
            vo.setValue(r.getAmount());
            vo.setType(r.getType());
            records.add(vo);
        }
        return records;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailySignIn signIn(Long userId) {
        LocalDate today = LocalDate.now();

        // 1. 获取或初始化钱包
        UserPointsWallet wallet = getOrCreateWallet(userId);

        // 2. 检查今日是否已签到
        if (today.equals(wallet.getLastSignDate())) {
            throw new BusinessException("今日已签到，请明天再来");
        }

        // 3. 计算连续签到天数
        int consecutiveDays;
        LocalDate yesterday = today.minusDays(1);
        if (yesterday.equals(wallet.getLastSignDate())) {
            consecutiveDays = wallet.getConsecutiveDays() + 1;
        } else {
            consecutiveDays = 1;
        }

        // 4. 计算签到积分（连续7天翻倍）
        int points = SIGN_IN_BASE_POINTS;
        if (consecutiveDays % 7 == 0) {
            points = SIGN_IN_BASE_POINTS * 2;
        }

        // 5. 插入签到记录
        DailySignIn signIn = new DailySignIn();
        signIn.setUserId(userId);
        signIn.setSignDate(today);
        signIn.setPoints(points);
        signIn.setConsecutiveDays(consecutiveDays);
        signInMapper.insert(signIn);

        // 6. 更新钱包
        wallet.setBalance(wallet.getBalance() + points);
        wallet.setTotalEarned(wallet.getTotalEarned() + points);
        wallet.setConsecutiveDays(consecutiveDays);
        wallet.setLastSignDate(today);
        walletMapper.updateById(wallet);

        // 7. 记录流水
        insertRecord(userId, 1, points, 1, null, "每日签到奖励");

        Map<String, Object> result = new HashMap<>();
        result.put("points", points);
        result.put("consecutiveDays", consecutiveDays);
        return signIn;
    }

    @Override
    public LotteryConfigVO getLotteryConfig() {
        LotteryConfigVO config = new LotteryConfigVO();
        config.setCostPerDraw(LOTTERY_COST);
        
        // 从数据库查询启用状态的奖品
        List<com.smartLive.points.domain.PointsLotteryPrize> dbPrizes = prizeMapper.selectList(
                new LambdaQueryWrapper<com.smartLive.points.domain.PointsLotteryPrize>()
                        .eq(com.smartLive.points.domain.PointsLotteryPrize::getStatus, 1)
                        .orderByAsc(com.smartLive.points.domain.PointsLotteryPrize::getSort));

        List<LotteryConfigVO.PrizeItem> prizeList = new ArrayList<>();
        for (com.smartLive.points.domain.PointsLotteryPrize p : dbPrizes) {
            LotteryConfigVO.PrizeItem item = new LotteryConfigVO.PrizeItem();
            item.setId(p.getId().intValue());
            item.setName(p.getName());
            item.setType(p.getType());
            item.setValue(p.getValue());
            item.setDesc(p.getDescription());
            prizeList.add(item);
        }
        config.setPrizes(prizeList);
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LotteryResultVO lotteryDraw(Long userId) {
        // 1. 检查余额
        UserPointsWallet wallet = getOrCreateWallet(userId);
        if (wallet.getBalance() < LOTTERY_COST) {
            throw new BusinessException("积分余额不足，当前余额：" + wallet.getBalance() + "，抽奖需要：" + LOTTERY_COST);
        }

        // 2. 获取奖品列表并计算权重
        List<com.smartLive.points.domain.PointsLotteryPrize> prizes = prizeMapper.selectList(
                new LambdaQueryWrapper<com.smartLive.points.domain.PointsLotteryPrize>()
                        .eq(com.smartLive.points.domain.PointsLotteryPrize::getStatus, 1)
                        .orderByAsc(com.smartLive.points.domain.PointsLotteryPrize::getSort));
        
        if (prizes.isEmpty()) {
            throw new BusinessException("抽奖活动暂未开启");
        }

        int[] weights = prizes.stream().mapToInt(com.smartLive.points.domain.PointsLotteryPrize::getProbability).toArray();

        // 3. 扣除积分
        wallet.setBalance(wallet.getBalance() - LOTTERY_COST);
        walletMapper.updateById(wallet);

        // 4. 记录扣除流水
        insertRecord(userId, 2, LOTTERY_COST, 2, null, "积分抽奖消耗");

        // 5. 执行抽奖
        int prizeIndex = weightedRandom(weights);
        com.smartLive.points.domain.PointsLotteryPrize prize = prizes.get(prizeIndex);

        // 6. 发放奖品
        if ("points".equals(prize.getType()) && prize.getValue() > 0) {
            // 重新获取钱包（乐观锁更新后需要重新查）
            wallet = walletMapper.selectById(userId);
            wallet.setBalance(wallet.getBalance() + prize.getValue());
            wallet.setTotalEarned(wallet.getTotalEarned() + prize.getValue());
            walletMapper.updateById(wallet);

            insertRecord(userId, 1, prize.getValue(), 1, null, "抽奖获得：" + prize.getName());
        }
        // coupon 类型可以在此处调用发券服务，暂不实现

        // 7. 返回结果
        LotteryResultVO result = new LotteryResultVO();
        result.setPrizeId(prize.getId().intValue());
        result.setPrizeName(prize.getName());
        result.setPrizeType(prize.getType());
        result.setPrizeValue(prize.getValue());
        return result;
    }

    /**
     * 新增积分（内部调用，如订单完成奖励）
     *
     * @param userId      用户ID
     * @param amount      积分数量
     * @param bizType     业务类型 (1:签到, 2:消费, 4:人工调整, 5:抽奖)
     * @param bizId       关联业务ID
     * @param description 展示文案
     */

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addPoints(Long userId, Integer amount, Integer bizType, String bizId, String description) {
        // 1. 获取或创建钱包
        UserPointsWallet wallet = getOrCreateWallet(userId);

        // 2. 增加积分
        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setTotalEarned(wallet.getTotalEarned() + amount);
        walletMapper.updateById(wallet);

        // 3. 记录流水
        insertRecord(userId, 1, amount, bizType, bizId, description);

        log.info("用户{}新增积分{}，业务类型:{}，业务ID:{}", userId, amount, bizType, bizId);
    }

    @Override
    public List<PointsRecord> selectRecordList(PointsRecord record) {
        LambdaQueryWrapper<PointsRecord> wrapper = new LambdaQueryWrapper<>();
        if (record.getUserId() != null) {
            wrapper.eq(PointsRecord::getUserId, record.getUserId());
        }
        if (record.getType() != null) {
            wrapper.eq(PointsRecord::getType, record.getType());
        }
        if (record.getBizType() != null && record.getBizType()!= 0) {
            wrapper.eq(PointsRecord::getBizType, record.getBizType());
        }
        wrapper.orderByDesc(PointsRecord::getCreateTime);
        return recordMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustPoints(Long userId, Integer type, Integer amount, String reason) {
        UserPointsWallet wallet = getOrCreateWallet(userId);

        if (type == 1) {
            // 充值/增加
            wallet.setBalance(wallet.getBalance() + amount);
            wallet.setTotalEarned(wallet.getTotalEarned() + amount);
            walletMapper.updateById(wallet);
            insertRecord(userId, 1, amount, 1, null, reason);
        } else if (type == 2) {
            // 扣减
            if (wallet.getBalance() < amount) {
                throw new BusinessException("扣减失败，用户当前余额：" + wallet.getBalance() + "，扣减数量：" + amount);
            }
            wallet.setBalance(wallet.getBalance() - amount);
            walletMapper.updateById(wallet);
            insertRecord(userId, 2, amount, 2, null, reason);
        } else {
            throw new BusinessException("无效的操作类型");
        }

        log.info("管理员调整用户{}积分，类型:{}，数量:{}，原因:{}", userId, type, amount, reason);
    }

    // ==================== 私有方法 ====================

    /**
     * 获取或创建用户钱包
     */
    private UserPointsWallet getOrCreateWallet(Long userId) {
        UserPointsWallet wallet = walletMapper.selectById(userId);
        if (wallet == null) {
            wallet = new UserPointsWallet();
            wallet.setUserId(userId);
            wallet.setBalance(0);
            wallet.setTotalEarned(0);
            wallet.setConsecutiveDays(0);
            wallet.setVersion(0);
            walletMapper.insert(wallet);
        }
        return wallet;
    }

    /**
     * 计算等级索引
     */
    private int calcLevelIndex(int totalEarned) {
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (totalEarned >= LEVEL_THRESHOLDS[i][0]) {
                return i;
            }
        }
        return 0;
    }

    /**
     * 插入积分流水
     */
    private void insertRecord(Long userId, int type, int amount, Integer bizType, String bizId, String description) {
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setType(type);
        record.setAmount(amount);
        record.setBizType(bizType);
        record.setBizId(bizId);
        record.setDescription(description);
        record.setCreateTime(LocalDateTime.now());
        recordMapper.insert(record);
    }

    /**
     * 加权随机算法
     */
    private int weightedRandom(int[] weights) {
        int totalWeight = 0;
        for (int w : weights) {
            totalWeight += w;
        }
        int rand = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            if (rand < cumulative) {
                return i;
            }
        }
        return weights.length - 1;
    }
}
