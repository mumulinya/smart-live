package com.smartLive.interaction.strategy.comment;

import com.smartLive.common.core.enums.common.ResourceTypeEnum;
import com.smartLive.interaction.service.IReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class ReviewCommentStrategy implements CommentStrategy {

    private final IReviewService reviewService;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.REVIEW_RESOURCE.getCode();
    }

    @Override
    public void transCommentCountFromRedis2DB(Map<Long, Integer> updateMap) {
        // 调用店铺服务的批量更新接口
        reviewService.updateCommentCountBatch(updateMap);
    }
}