package com.smartLive.interaction.controller.inner;

import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.interaction.domain.Review;
import com.smartLive.interaction.domain.VO.ShopReviewAnalysisVO;
import com.smartLive.interaction.domain.VO.ShopReviewSuggestVO;
import com.smartLive.interaction.service.IReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/inner/review")
public class ReviewInnerController extends BaseController {

    @Autowired
    private IReviewService reviewService;

    @GetMapping("/isReview")
    Boolean isReview(Review review) {
        return reviewService.isReview(review);
    }

    @PostMapping("/updateReviewStatus")
    Boolean updateReviewStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status, @RequestParam(value = "reason", required = false) String reason) {
        return reviewService.updateReviewStatus(id, status, reason);
    }

    @PostMapping("/saveAiCreateReview")
    public Boolean saveAiCreateReview(@RequestBody List<Review> reviews) {
        return reviewService.saveAiCreateReview(reviews);
    }

    @GetMapping("/analysis/{shopId}")
    public ShopReviewAnalysisVO getShopReviewAnalysis(@PathVariable("shopId") Long shopId,
                                                      @RequestParam("startTime") String startTime,
                                                      @RequestParam("endTime") String endTime) {
        return reviewService.getShopReviewAnalysis(shopId, startTime, endTime);
    }

    @GetMapping("/suggest/{shopId}")
    public ShopReviewSuggestVO getShopReviewSuggest(@PathVariable("shopId") Long shopId) {
        return reviewService.getShopReviewSuggest(shopId);
    }
}