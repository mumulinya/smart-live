package com.smartLive.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.ai.domain.AiMerchantMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiMerchantMessageMapper extends BaseMapper<AiMerchantMessage> {

    @Select("SELECT * FROM ai_merchant_message WHERE session_id = #{sessionId} ORDER BY create_time ASC, id ASC")
    List<AiMerchantMessage> selectBySessionId(Long sessionId);
}
