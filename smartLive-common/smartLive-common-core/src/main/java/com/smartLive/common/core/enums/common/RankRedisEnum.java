package com.smartLive.common.core.enums.common;
import com.smartLive.common.core.constant.InteractionRedisKeyConstants;

import com.smartLive.common.core.constant.RedisConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 集中管理互动模块（热榜/算分队列）Redis Key 映射机制。
 * 将原先穿插在 CommentTypeEnum 和 ReviewTypeEnum 中的持久层细节剥离，
 * 满足单一职责，更贴合策略模式分流。
 */
@Getter
@AllArgsConstructor
public enum RankRedisEnum {

    /** 博客类的热榜及队列配置 */
    BLOG_COMMENT_RANK(
            "COMMENT", GlobalBizTypeEnum.BLOG.getCode(),
            InteractionRedisKeyConstants.Comment.BLOG_COMMENT_HOT_RANK,
            InteractionRedisKeyConstants.Comment.BLOG_COMMENT_NEW_RANK,
            InteractionRedisKeyConstants.Comment.BLOG_COMMENT_CALC
    ),

    /** 评论类的热榜及队列配置 */
    COMMENT_COMMENT_RANK(
            "COMMENT", GlobalBizTypeEnum.COMMENT.getCode(),
            InteractionRedisKeyConstants.Comment.COMMENT_COMMENT_HOT_RANK,
            InteractionRedisKeyConstants.Comment.COMMENT_COMMENT_NEW_RANK,
            InteractionRedisKeyConstants.Comment.COMMENT_COMMENT_CALC
    ),

    /** 评价（作为主语时底下的评论）类的热榜及队列配置 */
    REVIEW_COMMENT_RANK(
            "COMMENT", GlobalBizTypeEnum.REVIEW.getCode(),
            InteractionRedisKeyConstants.Comment.REVIEW_COMMENT_HOT_RANK,
            InteractionRedisKeyConstants.Comment.REVIEW_COMMENT_NEW_RANK,
            InteractionRedisKeyConstants.Comment.REVIEW_COMMENT_CALC
    ),

    /** 商铺评价的热榜及队列配置 */
    SHOP_REVIEW_RANK(
            "REVIEW", GlobalBizTypeEnum.SHOP.getCode(),
            InteractionRedisKeyConstants.Review.SHOP_REVIEW_HOT_RANK,
            InteractionRedisKeyConstants.Review.SHOP_REVIEW_NEW_RANK,
            InteractionRedisKeyConstants.Review.SHOP_REVIEW_CALC
    ),

    /** 商品评价的热榜及队列配置 */
    PRODUCT_REVIEW_RANK(
            "REVIEW", GlobalBizTypeEnum.PRODUCT.getCode(),
            InteractionRedisKeyConstants.Review.PRODUCT_REVIEW_HOT_RANK,
            InteractionRedisKeyConstants.Review.PRODUCT_REVIEW_NEW_RANK,
            InteractionRedisKeyConstants.Review.PRODUCT_REVIEW_CALC
    ),

    // ================= 以下为实体（非评价/评论）自身的热榜队列映射 =================

    /** 博客自身的热榜队列映射 */
    BLOG_RANK(
            "ENTITY", GlobalBizTypeEnum.BLOG.getCode(),
            RedisConstants.BLOG_HOT_RANK_KEY,
            null,
            RedisConstants.BLOG_CALC_QUEUE_KEY
    ),

    /** 实体商城店铺自身的热榜队列映射 */
    SHOP_RANK(
            "ENTITY", GlobalBizTypeEnum.SHOP.getCode(),
            RedisConstants.SHOP_HOT_RANK_KEY,
            null,
            RedisConstants.SHOP_CALC_QUEUE_KEY
    ),

    /** 实体商品/团购自身的热榜队列映射 */
    PRODUCT_RANK(
            "ENTITY", GlobalBizTypeEnum.PRODUCT.getCode(),
            RedisConstants.PRODUCT_HOT_RANK_KEY,
            null,
            RedisConstants.PRODUCT_CALC_QUEUE_KEY
    );

    /** 热度计算对象分类：COMMENT, REVIEW, ENTITY */
    private final String category;

    /** 关联的业务模块编码 */
    private final Integer bizTypeCode;

    /** 热度排行榜 ZSet Key 前缀 */
    private final String hotRankKeyPrefix;
    /** 最新发布榜单 ZSet Key 前缀 */
    private final String newRankKeyPrefix;
    /** 等待重算的队列 Set Key */
    private final String calcQueueKey;

    public static RankRedisEnum getByCategoryAndCode(String category, Integer bizTypeCode) {
        if (category == null || bizTypeCode == null) {
            return null;
        }
        for (RankRedisEnum e : values()) {
            if (e.category.equals(category) && e.bizTypeCode.equals(bizTypeCode)) {
                return e;
            }
        }
        return null;
    }
}
