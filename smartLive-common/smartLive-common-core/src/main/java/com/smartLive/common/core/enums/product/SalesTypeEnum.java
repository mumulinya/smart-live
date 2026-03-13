package com.smartLive.common.core.enums.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 销量统计类型枚举
 */
@Getter
@AllArgsConstructor
public enum SalesTypeEnum {


    /** 商品销量统计策略 */
    PRODUCT_SALES("PRODUCT", "SALES:PRODUCT:COUNT:", "SALES:PRODUCT:DIRTY"),
    /** 店铺销量统计策略 */
    SHOP_SALES("SHOP", "SALES:SHOP:COUNT:", "SALES:SHOP:DIRTY");

    /** 业务类型代码 */
    private final String code;
    /** Redis 计数器 Key 前缀 */
    private final String countKeyPrefix;
    /** Redis 脏数据集合 Key (用于定时同步) */
    private final String dirtyKey;

    /**
     * 根据代码获取枚举
     * @param code 业务类型代码
     * @return 匹配的枚举，找不到返回 null
     */
    public static SalesTypeEnum getByCode(String code) {
        for (SalesTypeEnum value : values()) {
            if (value.getCode().equalsIgnoreCase(code)) {
                return value;
            }
        }
        return null;
    }
}
