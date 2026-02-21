package com.smartLive.common.core.enums;

import com.smartLive.common.core.constant.RedisConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 点赞类型与 Redis 键前缀映射
 */
@Getter
@AllArgsConstructor
public enum LikeTypeEnum {

    BLOG_LIKE(
            GlobalBizTypeEnum.BLOG.getCode(),
            "博客点赞",
            GlobalBizTypeEnum.BLOG.getBizDomain(),
            RedisConstants.BLOG_LIKED_KEY,
            RedisConstants.USER_BLOG_LIKED_KEY,
            RedisConstants.BLOG_LIKED_COUNT_KEY,
            RedisConstants.BLOG_LIKED_DIRTY_KEY
    ),
    COMMENT_LIKE(
            GlobalBizTypeEnum.COMMENT.getCode(),
            "评论点赞",
            GlobalBizTypeEnum.COMMENT.getBizDomain(),
            RedisConstants.COMMENT_LIKED_KEY,
            RedisConstants.USER_COMMENT_LIKED_KEY,
            RedisConstants.COMMENT_LIKED_COUNT_KEY,
            RedisConstants.COMMENT_LIKED_DIRTY_KEY
    ),
    REVIEW_LIKE(
            GlobalBizTypeEnum.REVIEW.getCode(),
            "评价点赞",
            GlobalBizTypeEnum.REVIEW.getBizDomain(),
            RedisConstants.REVIEW_LIKED_KEY,
            RedisConstants.USER_REVIEW_LIKED_KEY,
            RedisConstants.REVIEW_LIKED_COUNT_KEY,
            RedisConstants.REVIEW_LIKED_DIRTY_KEY
    );

    private final Integer code;
    private final String desc;
    private final String bizDomain;

    /**
     * 正向索引: 资源 -> 点赞用户
     */
    private final String likeKeyPrefix;

    /**
     * 反向索引: 用户 -> 点赞资源
     */
    private final String userLikedKeyPrefix;

    private final String likedCountKeyPrefix;
    private final String likeDirtyKeyPrefix;

    public static LikeTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (LikeTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
