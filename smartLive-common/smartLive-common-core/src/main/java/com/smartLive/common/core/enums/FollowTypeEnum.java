package com.smartLive.common.core.enums;
import com.smartLive.common.core.constant.InteractionRedisKeyConstants;

import com.smartLive.common.core.constant.RedisConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 关注与粉丝 Redis 键前缀映射
 */
@Getter
@AllArgsConstructor
public enum FollowTypeEnum {
    USER_IDENTITY(
            GlobalBizTypeEnum.USER.getCode(),
            "user",
            "用户信息",
            InteractionRedisKeyConstants.Follow.USER_FOLLOW_USER,
            InteractionRedisKeyConstants.Follow.USER_FANS,
            InteractionRedisKeyConstants.Follow.USER_FOLLOW_USER_DIRTY,
            InteractionRedisKeyConstants.Follow.USER_FANS_DIRTY,
            InteractionRedisKeyConstants.Follow.USER_FOLLOW_USER_COUNT,
            InteractionRedisKeyConstants.Follow.USER_FANS_COUNT
    ),
    SHOP_IDENTITY(
            GlobalBizTypeEnum.SHOP.getCode(),
            "shop",
            "店铺信息",
            InteractionRedisKeyConstants.Follow.USER_FOLLOW_SHOP,
            InteractionRedisKeyConstants.Follow.SHOP_FANS,
            InteractionRedisKeyConstants.Follow.USER_FOLLOW_SHOP_DIRTY,
            InteractionRedisKeyConstants.Follow.SHOP_FANS_DIRTY,
            InteractionRedisKeyConstants.Follow.USER_FOLLOW_SHOP_COUNT,
            InteractionRedisKeyConstants.Follow.SHOP_FANS_COUNT
    ),
    PRODUCT_IDENTITY(
            GlobalBizTypeEnum.PRODUCT.getCode(),
            "product",
            "商品信息",
            InteractionRedisKeyConstants.Follow.USER_FOLLOW_PRODUCT,
            InteractionRedisKeyConstants.Follow.PRODUCT_FANS,
            InteractionRedisKeyConstants.Follow.USER_FOLLOW_PRODUCT_DIRTY,
            InteractionRedisKeyConstants.Follow.PRODUCT_FANS_DIRTY,
            InteractionRedisKeyConstants.Follow.USER_FOLLOW_PRODUCT_COUNT,
            InteractionRedisKeyConstants.Follow.PRODUCT_FANS_COUNT
    );

    private final Integer code;
    private final String bizDomain;
    private final String desc;

    /**
     * 反向索引: 用户 -> 被关注对象列表
     */
    private final String followKeyPrefix;

    /**
     * 正向索引: 对象 -> 粉丝列表
     */
    private final String fansKeyPrefix;

    /**
     * 关注列表脏数据 Key
     */
    private final String followDirtyKeyPrefix;

    /**
     * 粉丝列表脏数据 Key
     */
    private final String fansDirtyKeyPrefix;

    /**
     * 关注计数 Key 前缀 (独立 String 计数器, 如 user:follow:user:count:{userId})
     */
    private final String followCountKeyPrefix;

    /**
     * 粉丝计数 Key 前缀 (独立 String 计数器, 如 user:fans:count:{sourceId})
     */
    private final String fansCountKeyPrefix;

    public static FollowTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (FollowTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
