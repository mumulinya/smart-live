package com.smartLive.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.ai.domain.UserAiMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 AI 消息数据访问层接口。
 *
 * 作者：smartLive
 */
@Mapper
public interface UserAiMessageMapper extends BaseMapper<UserAiMessage> {
}
