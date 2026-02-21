package com.smartLive.common.core.enums;

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
            RedisConstants.FOLLOW_USER_KEY,
            RedisConstants.FANS_USER_KEY,
            RedisConstants.FOLLOW_USER_DIRTY_KEY,
            RedisConstants.FANS_USER_DIRTY_KEY
    ),
    SHOP_IDENTITY(
            GlobalBizTypeEnum.SHOP.getCode(),
            "shop",
            "店铺信息",
            RedisConstants.FOLLOW_SHOP_KEY,
            RedisConstants.FANS_SHOP_KEY,
            RedisConstants.FOLLOW_SHOP_DIRTY_KEY,
            RedisConstants.FANS_SHOP_DIRTY_KEY
    ),
    PRODUCT_IDENTITY(
            GlobalBizTypeEnum.PRODUCT.getCode(),
            "product",
            "商品信息",
            RedisConstants.FOLLOW_PRODUCT_KEY,
            RedisConstants.FANS_PRODUCT_KEY,
            RedisConstants.FOLLOW_PRODUCT_DIRTY_KEY,
            RedisConstants.FANS_PRODUCT_DIRTY_KEY
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
