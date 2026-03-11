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
     * 获取随机推荐问题
     * 随机抽取 3 个预设的 AI 对话引导问题，显示在前端搜索框或聊天界面下方
     *
     * @return 包含问题内容列表的 Result
     */
    @GetMapping
    public Result getSuggestions() {
        List<AiSuggestQuestion> list = suggestQuestionService.getRandomSuggestions(3);
        // 仅返回内容字符串列表，符合前端 UI 渲染逻辑
        List<String> contents = list.stream().map(AiSuggestQuestion::getContent).collect(Collectors.toList());
        return Result.ok(contents);
    }
}
