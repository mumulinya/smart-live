package com.smartLive.common.core.enums;

import com.smartLive.common.core.constant.RedisConstants;
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
            RedisConstants.BLOG_COMMENT_HOT_RANK_KEY,
            RedisConstants.BLOG_COMMENT_COUNT_KEY,
            RedisConstants.USER_COMMENT_KEY,
            RedisConstants.BLOG_COMMENT_NEW_RANK_KEY,
            RedisConstants.BLOG_COMMENT_SYNC_KEY,
            RedisConstants.BLOG_COMMENT_CALC_KEY
    ),

    COMMENT_COMMENT(
            GlobalBizTypeEnum.COMMENT.getCode(),
            "评论回复",
            GlobalBizTypeEnum.COMMENT.getBizDomain(),
            RedisConstants.COMMENT_COMMENT_HOT_RANK_KEY,
            RedisConstants.COMMENT_COMMENT_COUNT_KEY,
            RedisConstants.USER_COMMENT_KEY,
            RedisConstants.COMMENT_COMMENT_NEW_RANK_KEY,
            RedisConstants.COMMENT_COMMENT_SYNC_KEY,
            RedisConstants.COMMENT_COMMENT_CALC_KEY
    ),

    REVIEW_COMMENT(
            GlobalBizTypeEnum.REVIEW.getCode(),
            "评价评论",
            GlobalBizTypeEnum.REVIEW.getBizDomain(),
            RedisConstants.REVIEW_COMMENT_HOT_RANK_KEY,
            RedisConstants.REVIEW_COMMENT_COUNT_KEY,
            RedisConstants.USER_COMMENT_KEY,
            RedisConstants.REVIEW_COMMENT_NEW_RANK_KEY,
            RedisConstants.REVIEW_COMMENT_SYNC_KEY,
            RedisConstants.REVIEW_COMMENT_CALC_KEY
    );

    private final Integer code;
    private final String desc;
    private final String bizDomain;

    /**
     * 热门榜前缀（ZSet）
     */
    private final String commentHotRankKeyPrefix;

    /**
     * 互动计数前缀（String）
     */
    private final String commentCountKeyPrefix;

    /**
     * 用户评论反向索引前缀（ZSet）
     */
    private final String userCommentKeyPrefix;

    /**
     * 最新榜前缀（ZSet）
     */
    private final String commentNewRankKeyPrefix;

    /**
     * 落库队列 key（Set）
     */
    private final String commentSyncKey;

    /**
     * 算分队列 key（Set）
     */
    private final String commentCalcKey;

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
