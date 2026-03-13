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
 * AI 鍐呭鐢熸垚 Controller
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
     * AI 鐢熸垚鍗氬姝ｆ枃
     * 鏍规嵁搴楅摵 ID 鍜岄鏍煎亸濂斤紝鐢熸垚閫傚悎灏忕孩涔︾瓑骞冲彴鐨勬帰搴楁枃妗?
     *
     * @param dto 鍗氬鐢熸垚璇锋眰鍙傛暟锛屽寘鍚?shopId銆佹弿杩板拰椋庢牸
     * @return 鍖呭惈鏍囬鍒楄〃鍜屾鏂囧唴瀹圭殑 Result
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
     * AI 鐢熸垚璇勪环鍐呭
     * 妯℃嫙鐢ㄦ埛鍙ｅ惢锛屾牴鎹簵閾虹壒鑹茬敓鎴愮湡瀹炵殑娑堣垂璇勪环
     *
     * @param dto 璇勪环鐢熸垚璇锋眰鍙傛暟锛屽寘鍚?shopId 鍜岀浉鍏虫弿杩?
     * @return 鐢熸垚鐨勮瘎浠锋枃鏈唴瀹?
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
