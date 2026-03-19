package com.smartLive.interaction.strategy.like;

import com.smartLive.common.core.constant.UserResourceActionTypeConstants;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.common.ResourceTypeEnum;
import com.smartLive.interaction.service.IReviewService;
import com.smartLive.interaction.strategy.AbstractInteractionStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 评价点赞策略实现
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class ReviewLikeStrategy extends AbstractInteractionStrategy implements LikeStrategy {

    private final IReviewService reviewService;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.REVIEW_RESOURCE.getCode();
    }

    @Override
    public void transLikeCountFromRedis2DB(Map<Long, Integer> updateMap) {
        if (updateMap == null || updateMap.isEmpty()) {
            return;
        }

        try {
            Boolean result = reviewService.updateLikeCountBatch(updateMap);
            if (!result) {
                throw new RuntimeException("评价点赞数更新失败");
            }
        } catch (Exception e) {
            log.error("评价点赞数同步失败", e);
        }
    }

    @Override
    public Integer getLikeCount(Long sourceId) {
        if (sourceId == null || sourceId <= 0) {
            return 0;
        }

        try {
            return reviewService.getReviewLikeCount(sourceId);
        } catch (Exception e) {
            log.error("获取评价点赞数异常: sourceId={}", sourceId, e);
            return 0;
        }
    }

    @Override
    protected Object getSourceData(Long sourceId) {
        return reviewService.getReviewById(sourceId);
    }

    @Override
    protected String getBizDomain() {
        return GlobalBizTypeEnum.REVIEW.getBizDomain();
    }

    @Override
    protected String getActionType() {
        return UserResourceActionTypeConstants.USER_RESOURCE_ACTION_LIKE;
    }
}
