package com.smartLive.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.ai.domain.AiSuggestQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI 推荐问题数据访问层接口。
 *
 * 作者：smartLive
 */
@Mapper
public interface AiSuggestQuestionMapper extends BaseMapper<AiSuggestQuestion> {

    /**
     * 获取随机推荐问题。
     * @param count 返回的推荐问题数量
     * @return 推荐问题列表
     */
    @Select("SELECT * FROM ai_suggest_question WHERE status = 1 ORDER BY RAND() LIMIT #{count}")
    List<AiSuggestQuestion> getRandomSuggestions(int count);
}
