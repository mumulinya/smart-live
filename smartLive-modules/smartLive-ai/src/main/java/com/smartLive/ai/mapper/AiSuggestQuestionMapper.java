package com.smartLive.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.ai.domain.AiSuggestQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI Suggest Question Mapper
 *
 * @author smartLive
 */
@Mapper
public interface AiSuggestQuestionMapper extends BaseMapper<AiSuggestQuestion> {

    /**
     * Get random suggested questions
     * @param count number of suggestions to return
     * @return list of questions
     */
    @Select("SELECT * FROM ai_suggest_question WHERE status = 1 ORDER BY RAND() LIMIT #{count}")
    List<AiSuggestQuestion> getRandomSuggestions(int count);
}
