package com.smartLive.points.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.points.domain.DailySignIn;
import org.apache.ibatis.annotations.Mapper;

/**
 * 每日签到记录Mapper
 *
 * @author smartLive
 */
@Mapper
public interface DailySignInMapper extends BaseMapper<DailySignIn> {
}
