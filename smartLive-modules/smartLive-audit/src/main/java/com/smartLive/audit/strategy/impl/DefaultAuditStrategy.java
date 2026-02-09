package com.smartLive.audit.strategy.impl;

import com.smartLive.audit.domain.AuditTask;
import com.smartLive.audit.strategy.AuditStrategy;
import com.smartLive.user.api.RemoteAppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Default Audit Strategy
 * Used for types that don't have specific risk logic.
 */
@Component
public class DefaultAuditStrategy implements AuditStrategy {
    @Autowired
    private RemoteAppUserService remoteAppUserService;

    @Override
    public Integer getBizType() {
        return -1; // Represents default/fallback
    }

    @Override
    public boolean isHighRisk(AuditTask task) {
        return false;
    }

    @Override
    public void handleAuditResult(Long targetId, Integer status, String reason) {
        // Do nothing for default
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
