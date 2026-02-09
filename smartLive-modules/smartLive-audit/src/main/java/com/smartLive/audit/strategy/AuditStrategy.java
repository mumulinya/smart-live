package com.smartLive.audit.strategy;

import com.smartLive.audit.domain.AuditTask;
import com.smartLive.user.api.RemoteAppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Audit Strategy Interface
 * Handles business-specific logic for different audit types.
 */
@Component
public interface AuditStrategy {

    /**
     * Returns the business type this strategy handles.
     * @return Business Type ID (e.g., 4 for Voucher)
     */
    Integer getBizType();

    /**
     * Analyzes the task to determine if it is high risk.
     * @param task The audit task containing content snapshot
     * @return true if high risk, false otherwise
     */
    boolean isHighRisk(AuditTask task);

    /**
     * Handle audit result callback
     *
     * @param targetId The business ID (e.g., blogId, voucherId)
     * @param status   The audit status (e.g., 1: Pass, 2: Reject)
     * @param reason   The rejection reason
     */
    void handleAuditResult(Long targetId, Integer status, String reason);
    /**
     * Get submitter name
     * @param submitterId
     * @return
     */
    public String getSubmitterName(Long submitterId);
}
