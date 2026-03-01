package com.smartLive.audit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.audit.domain.AuditTask;
import com.smartLive.audit.domain.vo.AuditTaskVO;
import com.smartLive.common.rabbitmq.domain.AuditMessage;

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
     * 获取审核任务详情
     *
     * @param id 任务ID
     * @return 审核任务VO
     */
    AuditTaskVO getAuditDetail(Long id);

    /**
     * 审核操作
     *
     * @param id 任务ID
     * @param status 状态(1-审核通过, 2-人工驳回, 3-自动驳回)
     * @param reason 驳回原因
     * @return 结果
     */
    boolean auditAction(Long id, Integer status, String reason);

    /**
     * 创建审核任务
     * @param auditMessage 审核消息
     */
    Long createAuditTask(com.smartLive.common.rabbitmq.domain.AuditMessage auditMessage);
    /**
     * 处理审核任务
     * @param auditMessage 审核消息
     */
    void handleAudit(AuditMessage auditMessage);
}
