package com.smartLive.ai.controller;

import com.smartLive.ai.entity.vo.BlogGenerateVO;
import com.smartLive.ai.domain.DTO.BlogGenerateDTO;
import com.smartLive.ai.domain.DTO.ReviewGenerateDTO;
import com.smartLive.ai.service.generate.IBlogGenerateService;
import com.smartLive.ai.service.generate.IReviewGenerateService;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 内容生成控制器。
 *
 * 作者：smartLive
 */
@Slf4j
@RestController
@RequestMapping("/generate")
public class AiGenerateController extends BaseController {

    @Autowired
    private IBlogGenerateService blogGenerateService;

    @Autowired
    private IReviewGenerateService reviewGenerateService;

    /**
     * AI 生成博客正文。
     * 根据店铺 ID 和风格偏好，生成适合小红书等平台的探店文案。
     *
     * @param dto 博客生成请求参数，包含 shopId、描述和风格。
     * @return 包含标题列表和正文内容的结果对象
     */
    @PostMapping("/blog")
    public Result generateBlog(@RequestBody BlogGenerateDTO dto) {
        if (dto.getShopId() == null) {
            return Result.fail("璇锋彁渚涘簵閾篒D");
        }
        try {
            BlogGenerateVO vo = blogGenerateService.generate(dto);
            return Result.ok(vo);
        } catch (Exception e) {
            log.error("AI 鍗氬鐢熸垚澶辫触", e);
            return Result.fail("AI 鍗氬鐢熸垚澶辫触: " + e.getMessage());
        }
    }

    /**
     * AI 生成评价内容。
     * 模拟用户口吻，根据店铺特色生成真实自然的消费评价。
     *
     * @param dto 评价生成请求参数，包含 shopId 和相关描述。
     * @return 生成后的评价文本
     */
    @PostMapping("/review")
    public Result generateReview(@RequestBody ReviewGenerateDTO dto) {
        if (dto.getShopId() == null) {
            return Result.fail("璇锋彁渚涘簵閾篒D");
        }
        try {
            String content = reviewGenerateService.generate(dto);
            return Result.ok(content);
        } catch (Exception e) {
            log.error("AI 璇勪环鐢熸垚澶辫触", e);
            return Result.fail("AI 璇勪环鐢熸垚澶辫触: " + e.getMessage());
        }
    }
}
