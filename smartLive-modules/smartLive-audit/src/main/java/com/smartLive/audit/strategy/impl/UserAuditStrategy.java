package com.smartLive.audit.strategy.impl;

import com.smartLive.audit.domain.AuditTask;
import com.smartLive.audit.strategy.AuditStrategy;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.user.api.RemoteAppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Audit Strategy for Users
 */
@Component
public class UserAuditStrategy implements AuditStrategy {
    @Autowired
    private RemoteAppUserService remoteAppUserService;

    @Override
    public Integer getBizType() {
        return GlobalBizTypeEnum.USER.getCode();
    }

    @Override
    public boolean isHighRisk(AuditTask task) {
        // TODO: Add specific risk logic for users
        return false;
    }

    @Override
    public void handleAuditResult(Long targetId, Integer status, String reason) {
        // TODO: Call remote user service
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
