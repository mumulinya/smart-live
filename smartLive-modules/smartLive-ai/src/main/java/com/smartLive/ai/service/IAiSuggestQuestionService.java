package com.smartLive.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.ai.domain.AiSuggestQuestion;

import java.util.List;

/**
 * AI 推荐问题服务接口。
 *
 * 作者：smartLive
 */
public interface IAiSuggestQuestionService extends IService<AiSuggestQuestion> {

    /**
     * 获取随机启用的推荐问题列表。
     *
     * @param count 需要返回的问题数量
     * @return 推荐问题列表
     */
    List<AiSuggestQuestion> getRandomSuggestions(int count);
}
