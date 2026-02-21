package com.smartLive.common.core.enums;

import com.smartLive.common.core.constant.RedisConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 收藏类型与 Redis 键前缀映射
 */
@Getter
@AllArgsConstructor
public enum StarTypeEnum {

    BLOG_STAR(
            GlobalBizTypeEnum.BLOG.getCode(),
            "博客收藏",
            GlobalBizTypeEnum.BLOG.getBizDomain(),
            RedisConstants.BLOG_STAR_KEY,
            RedisConstants.BLOG_STAR_SOURCE_KEY,
            RedisConstants.BLOG_STAR_COUNT_KEY,
            RedisConstants.BLOG_STAR_DIRTY_KEY
    ),
    SHOP_STAR(
            GlobalBizTypeEnum.SHOP.getCode(),
            "店铺收藏",
            GlobalBizTypeEnum.SHOP.getBizDomain(),
            RedisConstants.SHOP_STAR_KEY,
            RedisConstants.SHOP_STAR_SOURCE_KEY,
            RedisConstants.SHOP_STAR_COUNT_KEY,
            RedisConstants.SHOP_STAR_DIRTY_KEY
    ),
    PRODUCT_STAR(
            GlobalBizTypeEnum.PRODUCT.getCode(),
            "商品收藏",
            GlobalBizTypeEnum.PRODUCT.getBizDomain(),
            RedisConstants.PRODUCT_STAR_KEY,
            RedisConstants.PRODUCT_STAR_SOURCE_KEY,
            RedisConstants.PRODUCT_STAR_COUNT_KEY,
            RedisConstants.PRODUCT_STAR_DIRTY_KEY
    ),
    REVIEW_STAR(
            GlobalBizTypeEnum.REVIEW.getCode(),
            "评价收藏",
            GlobalBizTypeEnum.REVIEW.getBizDomain(),
            RedisConstants.REVIEW_STAR_KEY,
            RedisConstants.REVIEW_STAR_SOURCE_KEY,
            RedisConstants.REVIEW_STAR_COUNT_KEY,
            RedisConstants.REVIEW_STAR_DIRTY_KEY
    );

    private final Integer code;
    private final String desc;
    private final String bizDomain;

    /**
     * 反向索引: 用户 -> 收藏资源
     */
    private final String starKeyPrefix;

    /**
     * 正向索引: 资源 -> 收藏用户
     */
    private final String sourceStarKeyPrefix;

    private final String starCountKeyPrefix;
    private final String starDirtyKeyPrefix;

    public static StarTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (StarTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
