package com.smartLive.audit.strategy;

import com.smartLive.audit.domain.AuditTask;
import com.smartLive.user.api.RemoteAppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 默认审核策略
 * 用于没有专属规则的业务类型
 */
@Component
public class DefaultAuditStrategy implements AuditStrategy {
    @Autowired
    private RemoteAppUserService remoteAppUserService;

    /**
     * 获取业务类型编码
     *
     * @return 业务类型编码
     */
    @Override
    public Integer getBizType() {
        // -1 表示默认/兜底策略
        return -1;
    }

    /**
     * 判断是否为高风险内容
     *
     * @param task 审核任务
     * @return 是否高风险
     */
    @Override
    public boolean isHighRisk(AuditTask task) {
        return false;
    }

    /**
     * 处理审核结果（默认策略不做任何处理）
     *
     * @param targetId 目标编号
     * @param status   审核状态
     * @param reason   审核原因
     * @return 是否处理成功
     */
    @Override
    public boolean handleAuditResult(Long targetId, Integer status, String reason) {
        // 默认策略不处理业务状态
        return true;
    }
    /**
     * 获取提交人名称
     *
     * @param submitterId 提交人编号
     * @return 提交人名称
     */
    @Override
    public String getSubmitterName(Long submitterId) {
        return remoteAppUserService.getUserNameById(submitterId);
    }
}
