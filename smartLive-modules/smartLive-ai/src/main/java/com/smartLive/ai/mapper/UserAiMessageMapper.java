package com.smartLive.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.ai.domain.UserAiMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI Message Mapper
 *
 * @author smartLive
 */
@Mapper
public interface UserAiMessageMapper extends BaseMapper<UserAiMessage> {
}
