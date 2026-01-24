package com.smartLive.audit.domain.vo;

import com.smartLive.audit.domain.AuditTask;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 审核任务VO
 */
@Data
public class AuditTaskVO extends AuditTask {

    /**
     * 是否高风险
     */
    private Boolean isHighRisk;

    /**
     * 从Entity转换为VO，并计算isHighRisk
     */
    public static AuditTaskVO fromEntity(AuditTask entity) {
        if (entity == null) {
            return null;
        }
        AuditTaskVO vo = new AuditTaskVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setIsHighRisk(calculateHighRisk(entity));
        return vo;
    }

    private static Boolean calculateHighRisk(AuditTask entity) {
        Integer bizType = entity.getBizType();
        Map<String, Object> content = entity.getAuditContent();

        if (content == null) {
            return false;
        }

        // 检查是否为 VOUCHER(4) 或 GROUP_BUY(6)
        boolean isTargetType = GlobalBizTypeEnum.VOUCHER.getCode().equals(bizType) ||
                               GlobalBizTypeEnum.GROUP_BUY.getCode().equals(bizType);

        if (isTargetType) {
            try {
                // 尝试获取价格信息 (支持 camelCase 和 snake_case)
                Object priceObj = content.get("price");
                if (priceObj == null) priceObj = content.get("salePrice");
                if (priceObj == null) priceObj = content.get("sale_price");

                Object originalPriceObj = content.get("originalPrice"); 
                if (originalPriceObj == null) originalPriceObj = content.get("original_price");

                if (priceObj != null && originalPriceObj != null) {
                    double price = Double.parseDouble(priceObj.toString());
                    double originalPrice = Double.parseDouble(originalPriceObj.toString());

                    if (originalPrice > 0 && (price / originalPrice) < 0.1) {
                        return true;
                    }
                }
            } catch (Exception e) {
                // 转换异常忽略，视为非高风险
            }
        }
        return false;
    }
}
