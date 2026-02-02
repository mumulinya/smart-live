package com.smartLive.interaction.strategy.star;

import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.interaction.service.IReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class ReviewStarStrategy implements StarStrategy {

    private final IReviewService reviewService;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.REVIEW_RESOURCE.getCode();
    }
    /**
     * 同步收藏数据到DB
     *
     * @param updateMap
     */
    @Override
    public void transStarCountFromRedis2DB(Map<Long, Integer> updateMap) {
        log.info("正在调用代金券服务，同步数据");
        // 调用博客服务的批量更新接口
        Boolean b = reviewService.updateStarCountBatch(updateMap);
        if (b) {
            log.info("同步数据成功");
        } else {
            log.info("同步数据失败");
        }
    }

    /**
     * 获取收藏数
     *
     * @param sourceId 业务ID
     * @return 点赞数
     */
    @Override
    public Integer getStarCount(Long sourceId) {
        return reviewService.getReviewStarCount(sourceId);
    }
}