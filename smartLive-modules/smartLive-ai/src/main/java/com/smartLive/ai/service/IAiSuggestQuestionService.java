package com.smartLive.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.ai.domain.AiSuggestQuestion;

import java.util.List;

/**
 * AI Suggest Question Service Interface
 *
 * @author smartLive
 */
public interface IAiSuggestQuestionService extends IService<AiSuggestQuestion> {

    /**
     * Get a list of random active suggested questions
     *
     * @param count count of questions to return
     * @return list of questions
     */
    List<AiSuggestQuestion> getRandomSuggestions(int count);
}
