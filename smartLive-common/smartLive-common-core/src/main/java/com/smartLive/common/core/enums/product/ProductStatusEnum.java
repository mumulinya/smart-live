// ==================== product ====================

package com.smartLive.common.core.enums.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商品业务状态枚举
 */
@Getter
@AllArgsConstructor
public enum ProductStatusEnum {

    /** 已上架 */
    ON_SHELF(1, "已上架"),
    /** 已下架 */
    OFF_SHELF(2, "已下架"),
    /** 已过期 */
    EXPIRED(3, "已过期");

    private final Integer code;
    private final String desc;

    public static ProductStatusEnum getByCode(Integer code) {
        for (ProductStatusEnum value : values()) {
            if (value.getCode().equals(code)) return value;
        }
        return null;
    }
}