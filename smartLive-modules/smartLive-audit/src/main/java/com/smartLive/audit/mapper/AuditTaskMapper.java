package com.smartLive.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.audit.domain.AuditTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审核任务 Mapper 接口
 */
@Mapper
public interface AuditTaskMapper extends BaseMapper<AuditTask> {
}
