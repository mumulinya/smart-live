package com.smartLive.interaction.controller.inner;

import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.domain.Review;
import com.smartLive.interaction.service.IReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评价管理内部接口
 *
 * @author mumulin
 * @date 2025-09-21
 */
@RestController
@RequestMapping("/inner/review")
public class ReviewInnerController extends BaseController {

    @Autowired
    private IReviewService reviewService;

    /**
     * 判断当前用户是否评价过目标资源
     */
    @GetMapping("/isReview")
    Boolean isReview(Review review){
        return reviewService.isReview(review);
    }

    /**
     * 更新评价状态
     */
    @PostMapping("/updateReviewStatus")
    Boolean updateReviewStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status){
        return reviewService.updateReviewStatus(id, status);
    }
    /**
     * 保存AI创建的评价到Redis
     * @param reviews 评价列表
     * @return 操作结果
     */
    @PostMapping("/saveAiCreateReview")
    public Boolean saveAiCreateReview(@RequestBody List<Review> reviews){
        return reviewService.saveAiCreateReview(reviews);
    }
}
