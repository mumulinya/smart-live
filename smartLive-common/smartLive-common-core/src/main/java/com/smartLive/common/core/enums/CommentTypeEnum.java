package com.smartLive.common.core.enums;

import com.smartLive.common.core.constant.RedisConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 评论类型总枚举
 * 职责：
 * 1. 评论类型编码 (数据库存储值)
 * 2. 提供策略模式所需的 Bean 名称 (strategyName)
 * 3. 提供 MQ/API 用的业务域标识 (bizDomain)
 */
@Getter
@AllArgsConstructor
public enum CommentTypeEnum {

    // 1. 博客评论配置
    BLOG_COMMENT(GlobalBizTypeEnum.BLOG.getCode(), "博客的评论",GlobalBizTypeEnum.BLOG.getBizDomain(),
            RedisConstants.BLOG_COMMENT_KEY,
            RedisConstants.BLOG_COMMENT_COUNT_KEY,
            RedisConstants.BLOG_COMMENT_DIRTY_KEY,
            RedisConstants.USER_COMMENT_KEY),

    // 2. 评论评论配置
    COMMENT_COMMENT(GlobalBizTypeEnum.COMMENT.getCode(), "评论的评论",GlobalBizTypeEnum.COMMENT.getBizDomain(),
            RedisConstants.COMMENT_COMMENT_KEY,
            RedisConstants.COMMENT_COMMENT_COUNT_KEY,
            RedisConstants.COMMENT_COMMENT_DIRTY_KEY,
            RedisConstants.USER_COMMENT_KEY),
    // 2. 评价评论配置
    REVIEW_COMMENT(GlobalBizTypeEnum.REVIEW.getCode(), "评价的评论",GlobalBizTypeEnum.REVIEW.getBizDomain(),
            RedisConstants.REVIEW_COMMENT_KEY,
            RedisConstants.REVIEW_COMMENT_COUNT_KEY,
            RedisConstants.REVIEW_COMMENT_DIRTY_KEY,
            RedisConstants.USER_COMMENT_KEY );

    /**
     * 业务类型编码 (与 ResourceTypeEnum 保持一致)
     */
    private final Integer code;
    
    /**
     * 描述
     */
    private final String desc;
    
    /**
     * 业务域标识
     */
    private final String bizDomain;

    /**
     * 1. 用户点赞关系 Key (Set结构: 存userId)
     * e.g. likes:blog:101 -> {user1, user2}
     */
    private final String commentKeyPrefix;

    /**
     * 2. 点赞计数 Key (String结构: 存数字)
     * e.g. likes:count:blog:101 -> 99
     */
    private final String commentCountKeyPrefix;

    /**
     * 3. 脏数据 Key (Set结构: 存bizId，用于定时任务同步)
     * e.g. likes:dirty:blog -> {101, 102}
     */
    private final String commentDirtyKeyPrefix;

    /**
     * 4. 反向索引 Key (用户 -> 已评论源ID)
     */
    private final String userCommentKeyPrefix;

    /**
     * 根据 code 获取枚举
     * 如果传入不支持点赞的 code (如店铺 2)，这里直接返回 null
     */
    public static CommentTypeEnum getByCode(Integer code) {
        if (code == null) return null;
        for (CommentTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
