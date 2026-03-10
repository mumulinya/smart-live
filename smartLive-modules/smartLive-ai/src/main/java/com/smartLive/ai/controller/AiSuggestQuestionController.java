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
     * Get 3 random suggestions
     */
    @GetMapping
    public Result getSuggestions() {
        List<AiSuggestQuestion> list = suggestQuestionService.getRandomSuggestions(3);
        // Only return the content strings assuming frontend structure expects array of strings, 
        // or we return the full objects. Let's return strings per current UI expectation.
        List<String> contents = list.stream().map(AiSuggestQuestion::getContent).collect(Collectors.toList());
        return Result.ok(contents);
    }
}
