package com.smartLive.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.ai.domain.MerchantAiSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商家 AI 会话数据访问层接口。
 */
@Mapper
public interface MerchantAiSessionMapper extends BaseMapper<MerchantAiSession> {
}
