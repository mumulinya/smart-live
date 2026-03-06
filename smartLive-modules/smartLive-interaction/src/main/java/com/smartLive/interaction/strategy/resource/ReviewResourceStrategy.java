package com.smartLive.interaction.strategy.resource;

import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.interaction.domain.VO.ReviewVO;
import com.smartLive.interaction.service.IReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Component
public class ReviewResourceStrategy implements ResourceStrategy<ReviewVO> {

    @Autowired
    private IReviewService reviewService;

    /**
     * 策略标识 (USER / SHOP)
     */
    @Override
    public Integer getType() {
        return ResourceTypeEnum.REVIEW_RESOURCE.getCode();
    }

    /**
     * 获取资源列表
     *
     * @param sourceIdList
     */
    @Override
    public List<ReviewVO> getResourceList(List<Long> sourceIdList) {
        List<ReviewVO> reviewList = reviewService.getReviewListByIds(sourceIdList);
        if (reviewList.isEmpty()) {
            return null;
        }
        return reviewList;
    }

    /**
     * 获取资源
     *
     * @param sourceId
     */
    @Override
    public ReviewVO getResourceById(Long sourceId) {
        ReviewVO review = reviewService.getReviewById(sourceId);
        return review;
    }

    /**
     * 获取资源id
     *
     * @param data
     * @return
     */
    @Override
    public Long getResourceId(ReviewVO data) {
        return data.getId();
    }

    /**
     * 获取资源作者id
     */
    @Override
    public Long getAuthorId(ReviewVO data) {
        return data.getUserId();
    }

    /**
     * 获取资源内容
     *
     * @param sourceId
     */
    @Override
    public HashMap<String, String> getResourceContentById(Long sourceId) {
        ReviewVO review = reviewService.getReviewById(sourceId);
        HashMap<String, String> map = new HashMap<>();
        if (review != null && review.getContent() != null) {
            String content = review.getContent();
            map.put("title", content.length() > 20 ? content.substring(0, 20) : content);
        }
        if (review != null) {
            map.put("images", review.getImages());
        }
        return map; // Return the content of the comment
    }
}
