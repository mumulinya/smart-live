package com.smartLive.audit.strategy;

import com.smartLive.audit.domain.AuditTask;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.interaction.api.RemoteReviewService;
import com.smartLive.user.api.RemoteAppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 评价审核策略
 */
@Component
public class ReviewAuditStrategy implements AuditStrategy {
    @Autowired
    private RemoteAppUserService remoteAppUserService;
    @Autowired
    private RemoteReviewService remoteReviewService;

    /**
     * 获取业务类型编码
     *
     * @return 业务类型编码
     */
    @Override
    public Integer getBizType() {
        return GlobalBizTypeEnum.REVIEW.getCode();
    }

    /**
     * 判断是否为高风险内容
     *
     * @param task 审核任务
     * @return 是否高风险
     */
    @Override
    public boolean isHighRisk(AuditTask task) {
        // TODO: 添加评价高风险判断逻辑
        return false;
    }

    /**
     * 处理审核结果并更新评价状态
     *
     * @param targetId 目标评价编号
     * @param status   审核状态
     * @param reason   审核原因
     * @return 是否更新成功
     */
    @Override
    public boolean handleAuditResult(Long targetId, Integer status, String reason) {
        // 调用远程评价服务
        return remoteReviewService.updateReviewStatus(targetId, status, reason);
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
