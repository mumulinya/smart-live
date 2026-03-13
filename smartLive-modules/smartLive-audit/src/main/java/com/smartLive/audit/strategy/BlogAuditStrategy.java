package com.smartLive.audit.strategy;

import com.smartLive.audit.domain.AuditTask;
import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.user.api.RemoteAppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Audit Strategy for Blogs (Notes)
 * Currently no specific high-risk logic.
 */
@Component
public class BlogAuditStrategy implements AuditStrategy {

    @Autowired
    private RemoteBlogService remoteBlogService;
    @Autowired
    private RemoteAppUserService remoteAppUserService;

    @Override
    public Integer getBizType() {
        return GlobalBizTypeEnum.BLOG.getCode();
    }

    @Override
    public boolean isHighRisk(AuditTask task) {
        // TODO: Add specific risk logic for blogs (e.g., sensitive keywords)
        return false;
    }

    @Override
    public boolean handleAuditResult(Long targetId, Integer status, String reason) {
        return remoteBlogService.updateBlogStatus(targetId, status, reason);
    }
    /**
     * Get submitter name
     *
     * @param submitterId
     * @return
     */
    @Override
    public String getSubmitterName(Long submitterId) {
        return remoteAppUserService.getUserNameById(submitterId);
    }
}
