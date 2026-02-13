package com.smartLive.audit.strategy;

import com.smartLive.audit.domain.AuditTask;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.user.api.RemoteAppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Audit Strategy for Group Buy
 * Checks for deep discounts (price < 10% of original).
 */
@Component
public class GroupBuyAuditStrategy implements AuditStrategy {
    @Autowired
    private RemoteAppUserService remoteAppUserService;

    @Override
    public Integer getBizType() {
        return GlobalBizTypeEnum.GROUP_BUY.getCode();
    }

    @Override
    public boolean isHighRisk(AuditTask task) {
        Map<String, Object> content = task.getAuditContent();
        if (content == null) {
            return false;
        }

        // Extract price and original price
        Double price = getDoubleValue(content, "price", "salePrice", "sale_price");
        Double originalPrice = getDoubleValue(content, "originalPrice", "original_price");

        // Risk Logic: Price is less than 10% of original price
        if (price != null && originalPrice != null && originalPrice > 0) {
            return (price / originalPrice) < 0.1;
        }

        return false;
    }

    @Override
    public boolean handleAuditResult(Long targetId, Integer status, String reason) {
        // TODO: Call remote group buy service
        return true;
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
        return remoteAppUserService.getUserNameById(submitterId);
    }
}
