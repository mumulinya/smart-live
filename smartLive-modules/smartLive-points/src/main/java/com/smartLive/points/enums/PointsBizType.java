package com.smartLive.points.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 积分业务类型枚举
 *
 * @author smartLive
 */
@Getter
@AllArgsConstructor
public enum PointsBizType {

    /**
     * 签到
     */
    SIGN_IN(1, "签到"),

    /**
     * 消费
     */
    CONSUMPTION(2, "消费"),

    /**
     * 充值 (已弃用)
     */
    // RECHARGE(3, "充值"),

    /**
     * 人工调整
     */
    ADMIN_ADJUST(4, "人工调整"),

    /**
     * 抽奖
     */
    LOTTERY(5, "抽奖");

    private final Integer code;
    private final String desc;

    public static PointsBizType getByCode(Integer code) {
        for (PointsBizType value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
