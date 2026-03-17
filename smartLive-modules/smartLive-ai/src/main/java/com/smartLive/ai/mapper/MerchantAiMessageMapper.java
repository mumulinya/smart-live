package com.smartLive.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.ai.domain.MerchantAiMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商家 AI 消息数据访问层接口。
 */
@Mapper
public interface MerchantAiMessageMapper extends BaseMapper<MerchantAiMessage> {

    /**
     * 按会话 ID 查询商家 AI 消息列表。
     */
    @Select("SELECT * FROM merchant_ai_message WHERE session_id = #{sessionId} ORDER BY create_time ASC, id ASC")
    List<MerchantAiMessage> selectBySessionId(Long sessionId);
}
