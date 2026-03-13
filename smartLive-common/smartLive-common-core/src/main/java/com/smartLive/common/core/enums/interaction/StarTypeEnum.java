package com.smartLive.common.core.enums.interaction;
import com.smartLive.common.core.constant.InteractionRedisKeyConstants;

import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
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
            InteractionRedisKeyConstants.Star.USER_STAR_BLOG,
            InteractionRedisKeyConstants.Star.BLOG_STAR,
            InteractionRedisKeyConstants.Star.BLOG_STAR_COUNT,
            InteractionRedisKeyConstants.Star.BLOG_STAR_DIRTY
    ),
    SHOP_STAR(
            GlobalBizTypeEnum.SHOP.getCode(),
            "店铺收藏",
            GlobalBizTypeEnum.SHOP.getBizDomain(),
            InteractionRedisKeyConstants.Star.USER_STAR_SHOP,
            InteractionRedisKeyConstants.Star.SHOP_STAR,
            InteractionRedisKeyConstants.Star.SHOP_STAR_COUNT,
            InteractionRedisKeyConstants.Star.SHOP_STAR_DIRTY
    ),
    PRODUCT_STAR(
            GlobalBizTypeEnum.PRODUCT.getCode(),
            "商品收藏",
            GlobalBizTypeEnum.PRODUCT.getBizDomain(),
            InteractionRedisKeyConstants.Star.USER_STAR_PRODUCT,
            InteractionRedisKeyConstants.Star.PRODUCT_STAR,
            InteractionRedisKeyConstants.Star.PRODUCT_STAR_COUNT,
            InteractionRedisKeyConstants.Star.PRODUCT_STAR_DIRTY
    ),
    REVIEW_STAR(
            GlobalBizTypeEnum.REVIEW.getCode(),
            "评价收藏",
            GlobalBizTypeEnum.REVIEW.getBizDomain(),
            InteractionRedisKeyConstants.Star.USER_STAR_REVIEW,
            InteractionRedisKeyConstants.Star.REVIEW_STAR,
            InteractionRedisKeyConstants.Star.REVIEW_STAR_COUNT,
            InteractionRedisKeyConstants.Star.REVIEW_STAR_DIRTY
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
