package com.smartLive.common.core.enums.common;
import com.smartLive.common.core.constant.InteractionRedisKeyConstants;

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
            InteractionRedisKeyConstants.Comment.BLOG_COMMENT_HOT_RANK,
            InteractionRedisKeyConstants.Star.BLOG_STAR
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
            InteractionRedisKeyConstants.Star.PRODUCT_STAR
    ),
    COMMENT_RESOURCE(
            GlobalBizTypeEnum.COMMENT.getCode(),
            "评论资源",
            GlobalBizTypeEnum.COMMENT.getBizDomain(),
            InteractionRedisKeyConstants.Comment.COMMENT_COMMENT_HOT_RANK,
            null
    ),
    REVIEW_RESOURCE(
            GlobalBizTypeEnum.REVIEW.getCode(),
            "评价资源",
            GlobalBizTypeEnum.REVIEW.getBizDomain(),
            InteractionRedisKeyConstants.Comment.REVIEW_COMMENT_HOT_RANK,
            InteractionRedisKeyConstants.Star.REVIEW_STAR
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
