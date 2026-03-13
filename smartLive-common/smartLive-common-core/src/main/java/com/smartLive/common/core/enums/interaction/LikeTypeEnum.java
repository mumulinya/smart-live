package com.smartLive.common.core.enums.interaction;
import com.smartLive.common.core.constant.InteractionRedisKeyConstants;

import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
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
            InteractionRedisKeyConstants.Like.BLOG_LIKED,
            InteractionRedisKeyConstants.Like.USER_LIKED_BLOG,
            InteractionRedisKeyConstants.Like.BLOG_LIKED_COUNT,
            InteractionRedisKeyConstants.Like.BLOG_LIKED_DIRTY
    ),
    COMMENT_LIKE(
            GlobalBizTypeEnum.COMMENT.getCode(),
            "评论点赞",
            GlobalBizTypeEnum.COMMENT.getBizDomain(),
            InteractionRedisKeyConstants.Like.COMMENT_LIKED,
            InteractionRedisKeyConstants.Like.USER_LIKED_COMMENT,
            InteractionRedisKeyConstants.Like.COMMENT_LIKED_COUNT,
            InteractionRedisKeyConstants.Like.COMMENT_LIKED_DIRTY
    ),
    REVIEW_LIKE(
            GlobalBizTypeEnum.REVIEW.getCode(),
            "评价点赞",
            GlobalBizTypeEnum.REVIEW.getBizDomain(),
            InteractionRedisKeyConstants.Like.REVIEW_LIKED,
            InteractionRedisKeyConstants.Like.USER_LIKED_REVIEW,
            InteractionRedisKeyConstants.Like.REVIEW_LIKED_COUNT,
            InteractionRedisKeyConstants.Like.REVIEW_LIKED_DIRTY
    ),
    USER_LIKE(
            GlobalBizTypeEnum.USER.getCode(),
            "用户获赞总数",
            GlobalBizTypeEnum.USER.getBizDomain(),
            InteractionRedisKeyConstants.Like.USER_LIKED,
            null,
            InteractionRedisKeyConstants.Like.USER_LIKED_COUNT,
            InteractionRedisKeyConstants.Like.USER_LIKED_DIRTY
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
