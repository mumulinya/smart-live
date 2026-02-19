package com.smartLive.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商品类型枚举
 */
@Getter
@AllArgsConstructor
public enum ProductEnum {

    VOUCHER(1, "代金券"),

    SET_MEAL(2, "团购套餐");

    private final Integer code;
    private final String info;
}
