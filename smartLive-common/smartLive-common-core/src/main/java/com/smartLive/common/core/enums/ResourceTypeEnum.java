package com.smartLive.common.core.enums;

import com.smartLive.common.core.constant.FollowTypeConstants;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.constant.ResourceTypeConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResourceTypeEnum {

    // 格式：Code, Key, 描述, MQ后缀, 评论RedisKey, 收藏RedisKey, 点赞RedisKey

    // 1. 博客：支持评论、收藏、点赞。MQ后缀用 "blog" -> comment.create.blog
    BLOGRESOURCE(ResourceTypeConstants.BLOG, "blogSource", "博客资源", "blog",
            RedisConstants.BLOG_COMMENT_KEY, RedisConstants.BLOG_COLLECT_KEY, RedisConstants.BLOG_LIKED_KEY),

    // 2. 店铺：只支持评论(评分)。MQ后缀用 "shop" -> comment.create.shop
    // 注意：之前讨论过店铺通常不点赞不收藏，所以后面填 null
    SHOPRESOURCE(ResourceTypeConstants.SHOP, "shopSource", "店铺资源", "shop",
            RedisConstants.SHOP_COMMENT_KEY, null, null),

    // 3. 优惠券：MQ后缀用 "voucher"
    VOUCHERRESOURCE(ResourceTypeConstants.VOUCHER, "voucherSource", "优惠券资源", "voucher",
            RedisConstants.VOUCHER_COMMENT_KEY, RedisConstants.VOUCHER_COLLECT_KEY, null),

    // 4. 评论(嵌套评论)：MQ后缀用 "comment" (如果有这种需求的话)
    COMMENTRESOURCE(ResourceTypeConstants.COMMENT, "commentSource", "评论资源", "comment",
            RedisConstants.COMMENT_COMMENT_KEY, RedisConstants.COMMENT_COLLECT_KEY, RedisConstants.COMMENT_LIKED_KEY);


    private final Integer code;
    private final String key;       // 原有的key (带Source后缀)
    private final String desc;

    // ✅ 新增字段：专门给 MQ 用的简短后缀
    private final String mqPattern;

    // Redis Key 前缀配置
    private final String commentKeyPrefix;
    private final String collectKeyPrefix;
    private final String likeKeyPrefix;

    /**
     * 根据 code 获取枚举
     */
    public static ResourceTypeEnum getByCode(Integer code) {
        if (code == null) return null;
        for (ResourceTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}