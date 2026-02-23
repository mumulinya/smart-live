package com.smartLive.common.core.enums;

import com.smartLive.common.core.constant.RedisConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资源类型映射
 */
@Getter
@AllArgsConstructor
public enum ResourceTypeEnum {
    USER_RESOURCE(
            GlobalBizTypeEnum.USER.getCode(),
            "用户资源",
            GlobalBizTypeEnum.USER.getBizDomain(),
            null,
            null
    ),
    BLOG_RESOURCE(
            GlobalBizTypeEnum.BLOG.getCode(),
            "博客资源",
            GlobalBizTypeEnum.BLOG.getBizDomain(),
            RedisConstants.BLOG_COMMENT_HOT_RANK_KEY,
            RedisConstants.BLOG_STAR_SOURCE_KEY
    ),
    SHOP_RESOURCE(
            GlobalBizTypeEnum.SHOP.getCode(),
            "店铺资源",
            GlobalBizTypeEnum.SHOP.getBizDomain(),
            null,
            null
    ),
    PRODUCT_RESOURCE(
            GlobalBizTypeEnum.PRODUCT.getCode(),
            "商品资源",
            GlobalBizTypeEnum.PRODUCT.getBizDomain(),
            null,
            RedisConstants.PRODUCT_STAR_SOURCE_KEY
    ),
    COMMENT_RESOURCE(
            GlobalBizTypeEnum.COMMENT.getCode(),
            "评论资源",
            GlobalBizTypeEnum.COMMENT.getBizDomain(),
            RedisConstants.COMMENT_COMMENT_HOT_RANK_KEY,
            null
    ),
    REVIEW_RESOURCE(
            GlobalBizTypeEnum.REVIEW.getCode(),
            "评价资源",
            GlobalBizTypeEnum.REVIEW.getBizDomain(),
            RedisConstants.REVIEW_COMMENT_HOT_RANK_KEY,
            RedisConstants.REVIEW_STAR_SOURCE_KEY
    );

    private final Integer code;
    private final String desc;
    private final String bizDomain;
    private final String commentKeyPrefix;
    private final String collectKeyPrefix;

    public static ResourceTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ResourceTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
