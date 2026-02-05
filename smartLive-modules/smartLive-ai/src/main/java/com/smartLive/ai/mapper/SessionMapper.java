package com.smartLive.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.ai.domain.Session;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI Session Mapper
 *
 * @author smartLive
 */
@Mapper
public interface SessionMapper extends BaseMapper<Session> {
}
