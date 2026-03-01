package com.smartLive.common.core.enums;

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

    /** 正常（上架中） */
    NORMAL(0, "正常"),

    /** 已下架 */
    OFF_SHELF(1, "已下架"),

    /** 已删除 */
    DELETED(2, "已删除");

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
