package com.smartLive.interaction.api;

import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.interaction.api.DTO.ReviewDTO;
import com.smartLive.interaction.api.DTO.ShopReviewAnalysisDTO;
import com.smartLive.interaction.api.DTO.ShopReviewSuggestDTO;
import com.smartLive.interaction.api.factory.RemoteReviewFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(contextId = "remoteReviewService", value = ServiceNameConstants.INTERACTION_SERVICE, fallbackFactory = RemoteReviewFallbackFactory.class)
public interface RemoteReviewService {

    @GetMapping("/inner/review/isReview")
    Boolean isReview(@SpringQueryMap ReviewDTO reviewDTO);

    @PostMapping("/inner/review/updateReviewStatus")
    Boolean updateReviewStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status, @RequestParam(value = "reason", required = false) String reason);

    @PostMapping("/inner/review/saveAiCreateReview")
    Boolean saveAiCreateReview(@RequestBody List<ReviewDTO> reviews);

    @GetMapping("/inner/review/analysis/{shopId}")
    ShopReviewAnalysisDTO getShopReviewAnalysis(@PathVariable("shopId") Long shopId,
                                                @RequestParam("startTime") String startTime,
                                                @RequestParam("endTime") String endTime);

    @GetMapping("/inner/review/suggest/{shopId}")
    ShopReviewSuggestDTO getShopReviewSuggest(@PathVariable("shopId") Long shopId);
}