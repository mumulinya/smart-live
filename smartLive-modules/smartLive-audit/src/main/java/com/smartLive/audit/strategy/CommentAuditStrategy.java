package com.smartLive.audit.strategy;

import com.smartLive.audit.domain.AuditTask;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.interaction.api.RemoteCommentService;
import com.smartLive.user.api.RemoteAppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 评论审核策略
 */
@Component
public class CommentAuditStrategy implements AuditStrategy {
    @Autowired
    private RemoteAppUserService remoteAppUserService;

    @Autowired
    private RemoteCommentService remoteCommentService;

    /**
     * 获取业务类型编码
     *
     * @return 业务类型编码
     */
    @Override
    public Integer getBizType() {
        return GlobalBizTypeEnum.COMMENT.getCode();
    }

    /**
     * 判断是否为高风险内容
     *
     * @param task 审核任务
     * @return 是否高风险
     */
    @Override
    public boolean isHighRisk(AuditTask task) {
        // TODO: 添加评论高风险判断逻辑
        return false;
    }

    /**
     * 处理审核结果并更新评论状态
     *
     * @param targetId 目标评论编号
     * @param status   审核状态
     * @param reason   审核原因
     * @return 是否更新成功
     */
    @Override
    public boolean handleAuditResult(Long targetId, Integer status, String reason) {
        // 调用远程评论服务更新状态
        return remoteCommentService.updateCommentStatus(targetId, status);
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
