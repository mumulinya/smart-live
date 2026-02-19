package com.smartLive.product.service.strategy;

import com.smartLive.product.domain.Product;

/**
 * 购买策略接口
 *
 * @author 桃桃
 * @date 2026-02-18
 */
public interface PurchaseStrategy {
    /**
     * 执行购买逻辑
     *
     * @param userId 用户ID
     * @param product 商品信息
     * @return 订单ID
     */
    Long purchase(Long userId, Product product);
}
