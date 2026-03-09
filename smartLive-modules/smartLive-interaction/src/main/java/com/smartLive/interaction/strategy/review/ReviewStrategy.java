package com.smartLive.interaction.strategy.review;

import com.smartLive.interaction.domain.VO.ReviewVO;

import java.util.List;
import java.util.Map;

/**
 * 评价行为策略接口
 * 职责：处理点赞数据的持久化、同步等写操作
 */
public interface ReviewStrategy {

    /**
     * 获取策略类型 (对应 ResourceTypeEnum 的 code)
     */
    Integer getType();
    /**
     * 设置评价来源名称
     * @param reviewVO
     * @return
     */
    List<ReviewVO> setSourceName(List<ReviewVO> reviewVOs);

    /**
     * 批量同步评价数到数据库
      * @param updateMap key: 业务ID, value: 最新评价 数
     */
    void transReviewCountFromRedis2DB(Map<Long, Integer> updateMap);
}