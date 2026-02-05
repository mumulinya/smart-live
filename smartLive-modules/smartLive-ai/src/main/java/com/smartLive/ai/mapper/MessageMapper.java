package com.smartLive.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.ai.domain.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI Message Mapper
 *
 * @author smartLive
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
