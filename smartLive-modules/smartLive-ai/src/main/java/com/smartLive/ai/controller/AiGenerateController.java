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
 * AI 内容生成 Controller
 *
 * @author smartLive
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
     * AI 生成博客正文
     */
    @PostMapping("/blog")
    public Result generateBlog(@RequestBody BlogGenerateDTO dto) {
        if (dto.getShopId() == null) {
            return Result.fail("请提供店铺ID");
        }
        try {
            BlogGenerateVO vo = blogGenerateService.generate(dto);
            return Result.ok(vo);
        } catch (Exception e) {
            log.error("AI 博客生成失败", e);
            return Result.fail("AI 博客生成失败: " + e.getMessage());
        }
    }

    /**
     * AI 生成评价内容
     */
    @PostMapping("/review")
    public Result generateReview(@RequestBody ReviewGenerateDTO dto) {
        if (dto.getShopId() == null) {
            return Result.fail("请提供店铺ID");
        }
        try {
            String content = reviewGenerateService.generate(dto);
            return Result.ok(content);
        } catch (Exception e) {
            log.error("AI 评价生成失败", e);
            return Result.fail("AI 评价生成失败: " + e.getMessage());
        }
    }
}
