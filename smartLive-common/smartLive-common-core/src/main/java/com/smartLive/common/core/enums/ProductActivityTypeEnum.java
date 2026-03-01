package com.smartLive.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商品活动类型枚举
 *
 * @author smartLive
 */
@Getter
@AllArgsConstructor
public enum ProductActivityTypeEnum {

    /** 普通售卖（无活动） */
    NORMAL(0, "普通售卖"),

    /** 秒杀活动 */
    SECKILL(1, "秒杀活动");

    private final Integer code;
    private final String desc;

    /**
     * 根据 code 获取枚举
     */
    public static ProductActivityTypeEnum getByCode(Integer code) {
        for (ProductActivityTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
