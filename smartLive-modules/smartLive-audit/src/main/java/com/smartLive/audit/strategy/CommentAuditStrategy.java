package com.smartLive.audit.strategy;

import com.smartLive.audit.domain.AuditTask;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.interaction.api.RemoteCommentService;
import com.smartLive.user.api.RemoteAppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Audit Strategy for Comments
 */
@Component
public class CommentAuditStrategy implements AuditStrategy {
    @Autowired
    private RemoteAppUserService remoteAppUserService;

    @Autowired
    private RemoteCommentService remoteCommentService;

    @Override
    public Integer getBizType() {
        return GlobalBizTypeEnum.COMMENT.getCode();
    }

    @Override
    public boolean isHighRisk(AuditTask task) {
        // TODO: Add specific risk logic for comments
        return false;
    }

    @Override
    public boolean handleAuditResult(Long targetId, Integer status, String reason) {
        // Call remote comment service to update status
       return remoteCommentService.updateCommentStatus(targetId, status);
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
