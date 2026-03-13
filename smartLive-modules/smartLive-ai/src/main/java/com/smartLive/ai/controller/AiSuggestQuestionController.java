package com.smartLive.ai.controller;

import com.smartLive.ai.domain.AiSuggestQuestion;
import com.smartLive.ai.service.IAiSuggestQuestionService;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI Suggestion Controller
 *
 * @author smartLive
 */
@Slf4j
@RestController
@RequestMapping("/suggestions")
public class AiSuggestQuestionController extends BaseController {

    @Autowired
    private IAiSuggestQuestionService suggestQuestionService;

    /**
     * 鑾峰彇闅忔満鎺ㄨ崘闂
     * 闅忔満鎶藉彇 3 涓璁剧殑 AI 瀵硅瘽寮曞闂锛屾樉绀哄湪鍓嶇鎼滅储妗嗘垨鑱婂ぉ鐣岄潰涓嬫柟
     *
     * @return 鍖呭惈闂鍐呭鍒楄〃鐨?Result
     */
    @GetMapping
    public Result getSuggestions() {
        List<AiSuggestQuestion> list = suggestQuestionService.getRandomSuggestions(3);
        // 浠呰繑鍥炲唴瀹瑰瓧绗︿覆鍒楄〃锛岀鍚堝墠绔?UI 娓叉煋閫昏緫
        List<String> contents = list.stream().map(AiSuggestQuestion::getContent).collect(Collectors.toList());
        return Result.ok(contents);
    }
}
