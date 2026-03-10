package com.smartLive.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.ai.domain.AiSuggestQuestion;
import com.smartLive.ai.mapper.AiSuggestQuestionMapper;
import com.smartLive.ai.service.IAiSuggestQuestionService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI Suggest Question Service Implementation
 *
 * @author smartLive
 */
@Service
public class AiSuggestQuestionServiceImpl extends ServiceImpl<AiSuggestQuestionMapper, AiSuggestQuestion> implements IAiSuggestQuestionService {

    @Override
    public List<AiSuggestQuestion> getRandomSuggestions(int count) {
        if (count <= 0) {
            count = 3;
        }
        return baseMapper.getRandomSuggestions(count);
    }
}
