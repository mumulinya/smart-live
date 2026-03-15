package com.smartLive.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.ai.domain.MerchantAiMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MerchantAiMessageMapper extends BaseMapper<MerchantAiMessage> {

    @Select("SELECT * FROM merchant_ai_message WHERE session_id = #{sessionId} ORDER BY create_time ASC, id ASC")
    List<MerchantAiMessage> selectBySessionId(Long sessionId);
}
