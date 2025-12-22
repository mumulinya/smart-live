package com.smartLive.common.core.enums;

import com.smartLive.common.core.constant.RedisConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 点赞业务专用枚举
 * 作用：只定义支持点赞的资源类型，消除 Null 判断
 */
@Getter
@AllArgsConstructor
public enum LikeTypeEnum {

    // 1. 博客点赞配置
    BLOG_LIKE(GlobalBizTypeEnum.BLOG.getCode(), "博客点赞",GlobalBizTypeEnum.BLOG.getBizDomain(),
            RedisConstants.BLOG_LIKED_KEY, 
            RedisConstants.BLOG_LIKED_COUNT_KEY, 
            RedisConstants.BLOG_LIKED_DIRTY_KEY),

    // 2. 评论点赞配置
    COMMENT_LIKE(GlobalBizTypeEnum.COMMENT.getCode(), "评论点赞",GlobalBizTypeEnum.COMMENT.getBizDomain(),
            RedisConstants.COMMENT_LIKED_KEY, 
            RedisConstants.COMMENT_LIKED_COUNT_KEY, 
            RedisConstants.COMMENT_LIKED_DIRTY_KEY);

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
    private final String likeKeyPrefix;

    /**
     * 2. 点赞计数 Key (String结构: 存数字)
     * e.g. likes:count:blog:101 -> 99
     */
    private final String likedCountKeyPrefix;

    /**
     * 3. 脏数据 Key (Set结构: 存bizId，用于定时任务同步)
     * e.g. likes:dirty:blog -> {101, 102}
     */
    private final String likeDirtyKeyPrefix;

    /**
     * 根据 code 获取枚举
     * 如果传入不支持点赞的 code (如店铺 2)，这里直接返回 null
     */
    public static LikeTypeEnum getByCode(Integer code) {
        if (code == null) return null;
        for (LikeTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}