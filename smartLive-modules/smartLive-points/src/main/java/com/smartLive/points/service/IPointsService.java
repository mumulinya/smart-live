package com.smartLive.points.service;

import com.smartLive.points.domain.PointsRecord;
import com.smartLive.points.domain.vo.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 积分服务接口
 *
 * @author smartLive
 */
public interface IPointsService {

    /**
     * 获取积分中心信息
     *
     * @param userId 用户ID
     * @return 钱包信息
     */
    WalletInfoVO getWalletInfo(Long userId);

    /**
     * 获取积分流水列表
     *
     * @param userId   用户ID
     * @param page     页码
     * @param pageSize 每页数量
     * @param type     筛选类型 all/in/out
     * @return 分页结果
     */
    Map<String, Object> getRecordList(Long userId, Integer page, Integer pageSize, String type);

    /**
     * 执行签到
     *
     * @param userId 用户ID
     * @return 签到结果
     */
    Map<String, Object> signIn(Long userId);

    /**
     * 获取抽奖配置
     *
     * @return 抽奖配置
     */
    LotteryConfigVO getLotteryConfig();

    /**
     * 积分抽奖
     *
     * @param userId 用户ID
     * @return 抽奖结果
     */
    LotteryResultVO lotteryDraw(Long userId);

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
    void addPoints(Long userId, Integer amount, Integer bizType, String bizId, String description);

    /**
     * 管理端 - 查询积分流水列表
     *
     * @param record 查询条件（userId, type, bizType）
     * @return 流水列表
     */
    List<PointsRecord> selectRecordList(PointsRecord record);

    /**
     * 管理端 - 手动调整积分
     *
     * @param userId  用户ID
     * @param type    1:增加 2:扣减
     * @param amount  积分数量
     * @param reason  原因
     */
    void adjustPoints(Long userId, Integer type, Integer amount, String reason);
}
