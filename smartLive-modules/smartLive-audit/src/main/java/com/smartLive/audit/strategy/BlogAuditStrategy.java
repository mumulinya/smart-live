package com.smartLive.audit.strategy;

import com.smartLive.audit.domain.AuditTask;
import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.user.api.RemoteAppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 笔记审核策略
 * 当前暂无专属高风险判断逻辑
 */
@Component
public class BlogAuditStrategy implements AuditStrategy {

    @Autowired
    private RemoteBlogService remoteBlogService;
    @Autowired
    private RemoteAppUserService remoteAppUserService;

    /**
     * 获取业务类型编码
     *
     * @return 业务类型编码
     */
    @Override
    public Integer getBizType() {
        return GlobalBizTypeEnum.BLOG.getCode();
    }

    /**
     * 判断是否为高风险内容
     *
     * @param task 审核任务
     * @return 是否高风险
     */
    @Override
    public boolean isHighRisk(AuditTask task) {
        // TODO: 添加笔记高风险判断逻辑（例如敏感词）
        return false;
    }

    /**
     * 处理审核结果并更新笔记状态
     *
     * @param targetId 目标笔记编号
     * @param status   审核状态
     * @param reason   审核原因
     * @return 是否更新成功
     */
    @Override
    public boolean handleAuditResult(Long targetId, Integer status, String reason) {
        return remoteBlogService.updateBlogStatus(targetId, status, reason);
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
