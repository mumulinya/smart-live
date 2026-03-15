package com.smartLive.interaction.strategy.impl;

import com.smartLive.interaction.domain.VO.CommentVO;
import com.smartLive.interaction.strategy.comment.CommentStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Default CommentStrategy.
 */
@Component
public class DefaultCommentStrategy implements CommentStrategy {

    @Override
    public Integer getType() {
        return -1;
    }

    /**
     * 设置评价来源名称
     *
     * @param reviewVOs@return
     */
    @Override
    public List<CommentVO> setSourceName(List<CommentVO> reviewVOs) {
        return List.of();
    }

    @Override
    public void transCommentCountFromRedis2DB(Map<Long, Integer> updateMap) {
        // no-op
    }
}
