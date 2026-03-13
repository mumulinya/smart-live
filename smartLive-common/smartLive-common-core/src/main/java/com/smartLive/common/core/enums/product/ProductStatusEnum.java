package com.smartLive.common.core.enums.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商品状态枚举
 *
 * @author smartLive
 */
@Getter
@AllArgsConstructor
public enum ProductStatusEnum {

    /** 未审核 */
    PENDING(0, "未审核"),

    /** 已上架 */
    NORMAL(1, "已上架"),

    /** 已下架 */
    OFF_SHELF(2, "已下架"),

    /** 审核失败 */
    AUDIT_FAIL(3, "审核失败"),

    /** 已过期 */
    EXPIRED(4, "已过期");

    private final Integer code;
    private final String desc;

    /**
     * 根据 code 获取枚举
     */
    public static ProductStatusEnum getByCode(Integer code) {
        for (ProductStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}