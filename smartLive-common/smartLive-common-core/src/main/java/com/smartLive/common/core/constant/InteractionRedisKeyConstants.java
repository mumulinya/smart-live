package com.smartLive.common.core.constant;

/**
 * 互动模块 Redis Key 常量池
 * 采用领域分组 (DDD) 设计，包含正向索引、统计计数、脏数据标记与反向索引
 */
public interface InteractionRedisKeyConstants {

    /**
     * 1. 关注与粉丝数据 (社交关系)
     */
    interface Follow {
        // --- 主动关注 (反向索引) ---
        String USER_FOLLOW_USER = "user:follow:user:";
        String USER_FOLLOW_SHOP = "user:follow:shop:";
        String USER_FOLLOW_PRODUCT = "user:follow:product:";

        // --- 实体粉丝 (正向索引) ---
        String USER_FANS = "user:fans:";
        String SHOP_FANS = "shop:fans:";
        String PRODUCT_FANS = "product:fans:";

        // --- 关注/粉丝脏列表 ---
        String USER_FOLLOW_USER_DIRTY = "user:follow:user:dirty";
        String USER_FOLLOW_SHOP_DIRTY = "user:follow:shop:dirty";
        String USER_FOLLOW_PRODUCT_DIRTY = "user:follow:product:dirty";

        String USER_FANS_DIRTY = "user:fans:dirty";
        String SHOP_FANS_DIRTY = "shop:fans:dirty";
        String PRODUCT_FANS_DIRTY = "product:fans:dirty";
    }

    /**
     * 2. 点赞数据
     */
    interface Like {
        // --- 正向索引 (谁赞了实体) & 计数 & 脏数据 ---
        String BLOG_LIKED = "blog:liked:";
        String BLOG_LIKED_COUNT = "blog:liked:count:";
        String BLOG_LIKED_DIRTY = "blog:liked:dirty:";

        String COMMENT_LIKED = "comment:liked:";
        String COMMENT_LIKED_COUNT = "comment:liked:count:";
        String COMMENT_LIKED_DIRTY = "comment:liked:dirty:";

        String REVIEW_LIKED = "review:liked:";
        String REVIEW_LIKED_COUNT = "review:liked:count:";
        String REVIEW_LIKED_DIRTY = "review:liked:dirty:";

        // --- 反向索引 (用户个人的点赞历史) ---
        String USER_LIKED_BLOG = "user:liked:blog:";
        String USER_LIKED_COMMENT = "user:liked:comment:";
        String USER_LIKED_REVIEW = "user:liked:review:";
    }

    /**
     * 3. 收藏数据
     */
    interface Star {
        // --- 正向索引 (谁收藏了实体) & 计数 & 脏数据 ---
        String BLOG_STAR = "blog:star:";
        String BLOG_STAR_COUNT = "blog:star:count:";
        String BLOG_STAR_DIRTY = "blog:star:dirty:";

        String SHOP_STAR = "shop:star:";
        String SHOP_STAR_COUNT = "shop:star:count:";
        String SHOP_STAR_DIRTY = "shop:star:dirty:";

        String PRODUCT_STAR = "product:star:";
        String PRODUCT_STAR_COUNT = "product:star:count:";
        String PRODUCT_STAR_DIRTY = "product:star:dirty:";

        String REVIEW_STAR = "review:star:";
        String REVIEW_STAR_COUNT = "review:star:count:";
        String REVIEW_STAR_DIRTY = "review:star:dirty:";

        // --- 反向索引 (用户个人的收藏历史) ---
        String USER_STAR_BLOG = "user:star:blog:";
        String USER_STAR_SHOP = "user:star:shop:";
        String USER_STAR_PRODUCT = "user:star:product:";
        String USER_STAR_REVIEW = "user:star:review:";
    }

    /**
     * 4. 评论数据
     */
    interface Comment {
        // --- 实体下的评论列表 & 计数 & 脏数据 ---
        String BLOG_COMMENT_NEW_RANK = "blog:comment:new_rank:";
        String BLOG_COMMENT_HOT_RANK = "blog:comment:hot_rank:";
        String BLOG_COMMENT_COUNT = "blog:comment:count:";
        String BLOG_COMMENT_SYNC = "blog:comment:sync_ids";
        String BLOG_COMMENT_CALC = "blog:comment:calc_ids";

        String REVIEW_COMMENT_NEW_RANK = "review:comment:new_rank:";
        String REVIEW_COMMENT_HOT_RANK = "review:comment:hot_rank:";
        String REVIEW_COMMENT_COUNT = "review:comment:count:";
        String REVIEW_COMMENT_SYNC = "review:comment:sync_ids";
        String REVIEW_COMMENT_CALC = "review:comment:calc_ids";

        String COMMENT_COMMENT_NEW_RANK = "comment:comment:new_rank:";
        String COMMENT_COMMENT_HOT_RANK = "comment:comment:hot_rank:";
        String COMMENT_COMMENT_COUNT = "comment:comment:count:";
        String COMMENT_COMMENT_SYNC = "comment:comment:sync_ids";
        String COMMENT_COMMENT_CALC = "comment:comment:calc_ids";

        // --- 反向索引 (我的评论记录) ---
        String USER_COMMENT = "user:comment:";
    }

    /**
     * 5. 评价数据
     */
    interface Review {
        // --- 实体下的评价列表 & 计数 & 脏数据 ---
        String SHOP_REVIEW_HOT_RANK = "shop:review:hot_rank:";
        String SHOP_REVIEW_NEW_RANK = "shop:review:new_rank:";
        String SHOP_REVIEW_COUNT = "shop:review:count:";
        String SHOP_REVIEW_SYNC = "shop:review:sync_ids";
        String SHOP_REVIEW_CALC = "shop:review:calc_ids";

        String PRODUCT_REVIEW_HOT_RANK = "product:review:hot_rank:";
        String PRODUCT_REVIEW_NEW_RANK = "product:review:new_rank:";
        String PRODUCT_REVIEW_COUNT = "product:review:count:";
        String PRODUCT_REVIEW_SYNC = "product:review:sync_ids";
        String PRODUCT_REVIEW_CALC = "product:review:calc_ids";

        // --- 反向索引 (我的评价记录) ---
        String USER_REVIEW = "user:review:";
    }
}
