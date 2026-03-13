package com.smartLive.common.core.enums.interaction;
import com.smartLive.common.core.constant.InteractionRedisKeyConstants;

import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 评论类型枚举
 */
@Getter
@AllArgsConstructor
public enum CommentTypeEnum {

    BLOG_COMMENT(
            GlobalBizTypeEnum.BLOG.getCode(),
            "博客评论",
            GlobalBizTypeEnum.BLOG.getBizDomain(),
            InteractionRedisKeyConstants.Comment.BLOG_COMMENT_COUNT,
            InteractionRedisKeyConstants.Comment.USER_COMMENT,
            InteractionRedisKeyConstants.Comment.BLOG_COMMENT_SYNC
    ),

    COMMENT_COMMENT(
            GlobalBizTypeEnum.COMMENT.getCode(),
            "评论回复",
            GlobalBizTypeEnum.COMMENT.getBizDomain(),
            InteractionRedisKeyConstants.Comment.COMMENT_COMMENT_COUNT,
            InteractionRedisKeyConstants.Comment.USER_COMMENT,
            InteractionRedisKeyConstants.Comment.COMMENT_COMMENT_SYNC
    ),

    REVIEW_COMMENT(
            GlobalBizTypeEnum.REVIEW.getCode(),
            "评价评论",
            GlobalBizTypeEnum.REVIEW.getBizDomain(),
            InteractionRedisKeyConstants.Comment.REVIEW_COMMENT_COUNT,
            InteractionRedisKeyConstants.Comment.USER_COMMENT,
            InteractionRedisKeyConstants.Comment.REVIEW_COMMENT_SYNC
    );

    private final Integer code;
    private final String desc;
    private final String bizDomain;

    /**
     * 互动计数前缀（String）
     */
    private final String commentCountKeyPrefix;

    /**
     * 用户评论反向索引前缀（ZSet）
     */
    private final String userCommentKeyPrefix;

    /**
     * 落库队列 key（Set）
     */
    private final String commentSyncKey;

    public static CommentTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CommentTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
