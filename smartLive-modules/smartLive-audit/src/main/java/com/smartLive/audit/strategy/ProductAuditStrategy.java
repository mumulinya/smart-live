package com.smartLive.audit.strategy;

import com.smartLive.audit.domain.AuditTask;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.product.api.RemoteProductService;
import com.smartLive.shop.api.RemoteShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 商品审核策略
 * 主要识别异常折扣（售价低于原价 10%）
 */
@Component
public class ProductAuditStrategy implements AuditStrategy {

    @Autowired
    private RemoteProductService remoteProductService;
    @Autowired
    private RemoteShopService remoteShopService;

    /**
     * 获取业务类型编码
     *
     * @return 业务类型编码
     */
    @Override
    public Integer getBizType() {
        // 使用商品业务类型编码
        return GlobalBizTypeEnum.PRODUCT.getCode();
    }

    /**
     * 判断是否为高风险内容
     *
     * @param task 审核任务
     * @return 是否高风险
     */
    @Override
    public boolean isHighRisk(AuditTask task) {
        Map<String, Object> content = task.getAuditContent();
        if (content == null) {
            return false;
        }

        // 提取售价与原价，兼容不同字段名
        Double price = getDoubleValue(content, "price", "salePrice", "sale_price");
        Double originalPrice = getDoubleValue(content, "originalPrice", "original_price");

        // 风险规则：售价低于原价 10%
        if (price != null && originalPrice != null && originalPrice > 0) {
            return (price / originalPrice) < 0.1;
        }

        return false;
    }

    /**
     * 处理审核结果并更新商品状态
     *
     * @param targetId 目标商品编号
     * @param status   审核状态
     * @param reason   审核原因
     * @return 是否更新成功
     */
    @Override
    public boolean handleAuditResult(Long targetId, Integer status, String reason) {
        return remoteProductService.updateProductStatus(targetId, status, reason);
    }

    /**
     * 从 map 中按顺序解析 double 值
     *
     * @param map  目标数据
     * @param keys 可选字段名
     * @return 解析后的数值
     */
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
     * 获取提交人名称
     *
     * @param submitterId 提交人编号
     * @return 提交人名称
     */
    @Override
    public String getSubmitterName(Long submitterId) {
        return remoteShopService.getShopById(submitterId).getName();
    }

}
