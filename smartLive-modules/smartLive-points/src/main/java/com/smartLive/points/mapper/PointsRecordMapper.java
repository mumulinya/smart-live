package com.smartLive.points.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.points.domain.PointsRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 积分流水记录Mapper
 *
 * @author smartLive
 */
@Mapper
public interface PointsRecordMapper extends BaseMapper<PointsRecord> {
}
