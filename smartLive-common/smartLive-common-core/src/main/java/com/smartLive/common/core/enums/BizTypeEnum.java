package com.smartLive.common.core.enums;

import com.smartLive.common.core.constant.RedisConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 业务类型枚举
 * 用于：收藏表(sys_favorite)、点赞表、Feed流推送内容类型
 */
@Getter
@AllArgsConstructor
public enum BizTypeEnum {

    // === 内容类 ===
    BLOG(1, RedisConstants.BLOG_FEED_KEY, "博客"),

    // === 营销/商品类 ===
    COUPON(2, RedisConstants.VOUCHER_FEED_KEY, "代金券"),
    GOODS(3, RedisConstants.SECKILL_VOUCHER_FEED_KEY, "秒杀代金券");
    private final Integer code;
    private final String feedKeyPrefix; // "follow:user:", "follow:shop:"
    private final String desc;

    /**
     * 根据 code 获取枚举对象 (用于数据库值转枚举)
     */
    public static BizTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        // 使用 Stream 流查找，或者用增强 for 循环
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElse(null); // 或者抛出异常，看业务需求
    }

    /**
     * 校验类型是否合法
     */
    public static boolean isValid(Integer code) {
        return getByCode(code) != null;
    }
}