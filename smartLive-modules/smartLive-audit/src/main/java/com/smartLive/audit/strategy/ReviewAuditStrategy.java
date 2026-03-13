package com.smartLive.audit.strategy;

import com.smartLive.audit.domain.AuditTask;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.interaction.api.RemoteReviewService;
import com.smartLive.user.api.RemoteAppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Audit Strategy for Reviews
 */
@Component
public class ReviewAuditStrategy implements AuditStrategy {
    @Autowired
    private RemoteAppUserService remoteAppUserService;
    @Autowired
    private RemoteReviewService remoteReviewService;

    @Override
    public Integer getBizType() {
        return GlobalBizTypeEnum.REVIEW.getCode();
    }

    @Override
    public boolean isHighRisk(AuditTask task) {
        // TODO: Add specific risk logic for reviews
        return false;
    }

    @Override
    public boolean handleAuditResult(Long targetId, Integer status, String reason) {
        // Call remote review service
       return remoteReviewService.updateReviewStatus(targetId, status, reason);
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
