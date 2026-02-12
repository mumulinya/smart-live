package com.smartLive.audit.strategy.impl;

import com.smartLive.audit.domain.AuditTask;
import com.smartLive.audit.strategy.AuditStrategy;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.system.api.RemoteUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Audit Strategy for Shops
 */
@Component
public class ShopAuditStrategy implements AuditStrategy {

    @Autowired
    private RemoteShopService remoteShopService;
    @Autowired
    private RemoteUserService remoteUserService;

    @Override
    public Integer getBizType() {
        return GlobalBizTypeEnum.SHOP.getCode();
    }

    @Override
    public boolean isHighRisk(AuditTask task) {
        // TODO: Add specific risk logic for shops
        return false;
    }

    @Override
    public boolean handleAuditResult(Long targetId, Integer status, String reason) {
        // Call remote service to update status
        return remoteShopService.updateShopStatus(targetId, status);
    }

    /**
     * Get submitter name
     *
     * @param submitterId
     * @return
     */
    @Override
    public String getSubmitterName(Long submitterId) {
        return remoteUserService.getUserById(submitterId).getNickName();
    }
}
