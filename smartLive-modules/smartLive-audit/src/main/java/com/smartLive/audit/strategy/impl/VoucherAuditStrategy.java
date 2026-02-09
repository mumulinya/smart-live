package com.smartLive.audit.strategy.impl;

import com.smartLive.audit.domain.AuditTask;
import com.smartLive.audit.strategy.AuditStrategy;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.marketing.api.RemoteVoucherService;
import com.smartLive.shop.api.RemoteShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Audit Strategy for Vouchers
 * Checks for deep discounts (price < 10% of original).
 */
@Component
public class VoucherAuditStrategy implements AuditStrategy {

    @Autowired
    private RemoteVoucherService remoteVoucherService;
    @Autowired
    private RemoteShopService remoteShopService;

    @Override
    public Integer getBizType() {
        return GlobalBizTypeEnum.VOUCHER.getCode();
    }

    @Override
    public boolean isHighRisk(AuditTask task) {
        Map<String, Object> content = task.getAuditContent();
        if (content == null) {
            return false;
        }

        // Extract price and original price, handling potential key variations
        Double price = getDoubleValue(content, "price", "salePrice", "sale_price");
        Double originalPrice = getDoubleValue(content, "originalPrice", "original_price");

        // Risk Logic: Price is less than 10% of original price
        if (price != null && originalPrice != null && originalPrice > 0) {
            return (price / originalPrice) < 0.1;
        }

        return false;
    }

    @Override
    public void handleAuditResult(Long targetId, Integer status, String reason) {
        // TODO: Call remote service to update status
        // remoteVoucherService.updateVoucherStatus(targetId, status);
    }

    private Double getDoubleValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null) {
                try {
                    return Double.valueOf(val.toString());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    /**
     * Get submitter name
     *
     * @param submitterId
     * @return
     */
    @Override
    public String getSubmitterName(Long submitterId) {
        return remoteShopService.getShopById(submitterId).getName();
    }

}
