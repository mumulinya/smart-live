package com.smartLive.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.ai.domain.UserAiSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 AI 会话数据访问层接口。
 *
 * 作者：smartLive
 */
@Mapper
public interface UserAiSessionMapper extends BaseMapper<UserAiSession> {
}
