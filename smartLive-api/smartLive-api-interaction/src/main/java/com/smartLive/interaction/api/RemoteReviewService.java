package com.smartLive.interaction.api;

import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.interaction.api.DTO.ReviewDTO;
import com.smartLive.interaction.api.factory.RemoteReviewFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(contextId = "remoteReviewService", value = ServiceNameConstants.INTERACTION_SERVICE, fallbackFactory = RemoteReviewFallbackFactory.class)
public interface RemoteReviewService {

    /**
     * 判断当前用户是否评价过目标资源
     *
     * @param reviewDTO 评价查询条件（至少包含 sourceType/sourceId）
     * @return 是否评价过
     */
    @GetMapping("/inner/review/isReview")
    Boolean isReview(@SpringQueryMap ReviewDTO reviewDTO);

    /**
     * 更新评价状态
     */
    @PostMapping("/inner/review/updateReviewStatus")
    Boolean updateReviewStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status, @RequestParam(value = "reason", required = false) String reason);
    /**
     * 保存AI创建的评价到Redis
     * @param reviews 评价列表
     * @return 操作结果
     */
    @PostMapping("/inner/review/saveAiCreateReview")
    public Boolean saveAiCreateReview(@RequestBody List<ReviewDTO> reviews);
}
