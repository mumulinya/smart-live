package com.smartLive.audit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.audit.domain.AuditTask;
import com.smartLive.audit.domain.vo.AuditTaskVO;

import java.util.List;

/**
 * 审核服务接口
 */
public interface IAuditService extends IService<AuditTask> {

    /**
     * 查询审核任务列表
     *
     * @param auditTask 查询条件
     * @return 审核任务VO集合
     */
    List<AuditTaskVO> selectAuditList(AuditTask auditTask);

    /**
     * 审核操作
     *
     * @param id 任务ID
     * @param status 状态(1-通过, 2-驳回)
     * @param reason 驳回原因
     * @return 结果
     */
    boolean auditAction(Long id, Integer status, String reason);
}
