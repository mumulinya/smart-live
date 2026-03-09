package com.smartLive.interaction.strategy.impl;

import com.smartLive.interaction.domain.VO.ReviewVO;
import com.smartLive.interaction.strategy.review.ReviewStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Default ReviewStrategy.
 */
@Component
public class DefaultReviewStrategy implements ReviewStrategy {

    @Override
    public Integer getType() {
        return -1;
    }

    /**
     * 设置评价来源名称
     *
     * @param reviewVO
     * @return
     */
    @Override
    public List<ReviewVO> setSourceName(List<ReviewVO> reviewVO) {
        return null;
    }


    @Override
    public void transReviewCountFromRedis2DB(Map<Long, Integer> updateMap) {
        // no-op
    }
}
