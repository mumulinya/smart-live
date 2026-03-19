package com.smartLive.audit.strategy;

import com.smartLive.audit.domain.AuditTask;
import com.smartLive.user.api.RemoteAppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 审核策略接口
 * 负责处理不同业务类型的审核逻辑
 */
@Component
public interface AuditStrategy {

    /**
     * 获取当前策略支持的业务类型编码
     *
     * @return 业务类型编码
     */
    Integer getBizType();

    /**
     * 判断审核任务是否高风险
     *
     * @param task 审核任务
     * @return 是否高风险
     */
    boolean isHighRisk(AuditTask task);

    /**
     * 处理审核结果回调
     *
     * @param targetId 目标业务编号
     * @param status   审核状态
     * @param reason   驳回原因
     */
    boolean handleAuditResult(Long targetId, Integer status, String reason);
    /**
     * 获取提交人名称
     *
     * @param submitterId 提交人编号
     * @return 提交人名称
     */
    String getSubmitterName(Long submitterId);
}
