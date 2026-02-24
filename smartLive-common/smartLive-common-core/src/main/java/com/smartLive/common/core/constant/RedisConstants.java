package com.smartLive.common.core.constant;

public class RedisConstants {
    /**
     * 登录验证码
     */
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    /**
     * 登录用户
     */
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 30L;
    /**
     * 缓存空值
     */
    public static final Long CACHE_NULL_TTL = 2L;
    /**
     * 店铺缓存
     */
    public static final String CACHE_SHOP_KEY = "cache:shop:";
    public static final String CACHE_SHOP_lIST_KEY = "cache:shopList:";
    public static final Long CACHE_SHOP_TTL = 30L;
    /**
     * 店铺类型
     */
    public static final String CACHE_SHOP_TYPE_KEY = "cache:shopType";
    public static final Long CACHE_SHOP_TYPE_TTL = 30L;

    /**
     * 店铺锁
     */
    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;
    /**
     * 秒杀商品库存
     */
    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    /**
     * 博客缓存
     */
    public static final String CACHE_BLOG_KEY = "cache:blog:";
    public static final Long CACHE_BLOG_TTL = 30L;
    public static final String CACHE_HOT_BLOG_KEY = "cache:hotBlog:";
    public static final Long CACHE_HOT_BLOG_TTL = 1L;//1天
    /**
     * 博客类型
     */
    public static final String CACHE_BLOG_TYPE_KEY = "cache:blogType:";
    /**
     * 关注
     */
    //用户关注
    public static final String FOLLOW_USER_KEY = InteractionRedisKeyConstants.Follow.USER_FOLLOW_USER;
    //用户粉丝
    public static final String FANS_USER_KEY = InteractionRedisKeyConstants.Follow.USER_FANS;
    //店铺关注
    public static final String FOLLOW_SHOP_KEY = InteractionRedisKeyConstants.Follow.USER_FOLLOW_SHOP;
    //店铺粉丝
    public static final String FANS_SHOP_KEY = InteractionRedisKeyConstants.Follow.SHOP_FANS;
    //商品关注
    public static final String FOLLOW_PRODUCT_KEY = InteractionRedisKeyConstants.Follow.USER_FOLLOW_PRODUCT;
    //商品粉丝
    public static final String FANS_PRODUCT_KEY = InteractionRedisKeyConstants.Follow.PRODUCT_FANS;
    //关注/粉丝脏数据列表
    public static final String FOLLOW_USER_DIRTY_KEY = InteractionRedisKeyConstants.Follow.USER_FOLLOW_USER_DIRTY;
    public static final String FOLLOW_SHOP_DIRTY_KEY = InteractionRedisKeyConstants.Follow.USER_FOLLOW_SHOP_DIRTY;
    public static final String FOLLOW_PRODUCT_DIRTY_KEY = InteractionRedisKeyConstants.Follow.USER_FOLLOW_PRODUCT_DIRTY;
    public static final String FANS_USER_DIRTY_KEY = InteractionRedisKeyConstants.Follow.USER_FANS_DIRTY;
    public static final String FANS_SHOP_DIRTY_KEY = InteractionRedisKeyConstants.Follow.SHOP_FANS_DIRTY;
    public static final String FANS_PRODUCT_DIRTY_KEY = InteractionRedisKeyConstants.Follow.PRODUCT_FANS_DIRTY;
    /**
     * 商品缓存
     */
    public static final String CACHE_PRODUCT_KEY = "cache:product:";
    public static final Long CACHE_PRODUCT_TTL = 30L;

    /**
     * 用户缓存
     */
    public static final String CACHE_USER_KEY = "cache:user:";
    public static final Long CACHE_USER_TTL = 30L;
    /**
     * 评价缓存
     */
    public static final String CACHE_REVIEW_KEY = "cache:review:";
    public static final Long CACHE_REVIEW_TTL = 30L;
    public static final String CACHE_COMMENT_KEY = "cache:comment:";
    public static final Long CACHE_COMMENT_TTL = 30L;
    /**
     * 推送数据
     */
    //全部推送
    public static final String ALL_FEED_KEY = "feed:all:";
    //用户推送
    public static final String USER_FEED_KEY = "feed:user:";
    //店铺推送
    public static final String SHOP_FEED_KEY = "feed:shop:";
    //商品推送
    public static final String ITEM_FEED_KEY = "feed:item:";



    /**
     * 点赞数据
     */
    //博文点赞列表
    public static final String BLOG_LIKED_KEY = InteractionRedisKeyConstants.Like.BLOG_LIKED;
    public static final String USER_BLOG_LIKED_KEY = InteractionRedisKeyConstants.Like.USER_LIKED_BLOG;
    //博客点赞总数
    public static final String BLOG_LIKED_COUNT_KEY = InteractionRedisKeyConstants.Like.BLOG_LIKED_COUNT;
    //博客点赞脏数据列表
    public static final String BLOG_LIKED_DIRTY_KEY = InteractionRedisKeyConstants.Like.BLOG_LIKED_DIRTY;

    //评论点赞
    public static final String COMMENT_LIKED_KEY = InteractionRedisKeyConstants.Like.COMMENT_LIKED;
    public static final String USER_COMMENT_LIKED_KEY = InteractionRedisKeyConstants.Like.USER_LIKED_COMMENT;
    //评论点赞总数
    public static final String COMMENT_LIKED_COUNT_KEY = InteractionRedisKeyConstants.Like.COMMENT_LIKED_COUNT;
    //评论点赞脏数据列表
    public static final String COMMENT_LIKED_DIRTY_KEY = InteractionRedisKeyConstants.Like.COMMENT_LIKED_DIRTY;

    //评价点赞
    public static final String REVIEW_LIKED_KEY = InteractionRedisKeyConstants.Like.REVIEW_LIKED;
    public static final String USER_REVIEW_LIKED_KEY = InteractionRedisKeyConstants.Like.USER_LIKED_REVIEW;
    //评价点赞总数
    public static final String REVIEW_LIKED_COUNT_KEY = InteractionRedisKeyConstants.Like.REVIEW_LIKED_COUNT;
    //评价点赞脏数据列表
    public static final String REVIEW_LIKED_DIRTY_KEY = InteractionRedisKeyConstants.Like.REVIEW_LIKED_DIRTY;
    /**
     * 收藏数据
     */
    //博文收藏
    public static final String BLOG_STAR_KEY = InteractionRedisKeyConstants.Star.USER_STAR_BLOG;
    public static final String BLOG_STAR_SOURCE_KEY = InteractionRedisKeyConstants.Star.BLOG_STAR;
    //博客收藏总数
    public static final String BLOG_STAR_COUNT_KEY = InteractionRedisKeyConstants.Star.BLOG_STAR_COUNT;
    //博客收藏脏数据列表
    public static final String BLOG_STAR_DIRTY_KEY = InteractionRedisKeyConstants.Star.BLOG_STAR_DIRTY;

    //店铺收藏
    public static final String SHOP_STAR_KEY = InteractionRedisKeyConstants.Star.USER_STAR_SHOP;
    public static final String SHOP_STAR_SOURCE_KEY = InteractionRedisKeyConstants.Star.SHOP_STAR;
    //店铺收藏总数
    public static final String SHOP_STAR_COUNT_KEY = InteractionRedisKeyConstants.Star.SHOP_STAR_COUNT;
    //店铺收藏脏数据列表
    public static final String SHOP_STAR_DIRTY_KEY = InteractionRedisKeyConstants.Star.SHOP_STAR_DIRTY;

    //商品收藏
    public static final String PRODUCT_STAR_KEY = InteractionRedisKeyConstants.Star.USER_STAR_PRODUCT;
    public static final String PRODUCT_STAR_SOURCE_KEY = InteractionRedisKeyConstants.Star.PRODUCT_STAR;
    //商品收藏总数
    public static final String PRODUCT_STAR_COUNT_KEY = InteractionRedisKeyConstants.Star.PRODUCT_STAR_COUNT;
    //商品收藏脏数据列表
    public static final String PRODUCT_STAR_DIRTY_KEY = InteractionRedisKeyConstants.Star.PRODUCT_STAR_DIRTY;

    //评价收藏
    public static final String REVIEW_STAR_KEY = InteractionRedisKeyConstants.Star.USER_STAR_REVIEW;
    public static final String REVIEW_STAR_SOURCE_KEY = InteractionRedisKeyConstants.Star.REVIEW_STAR;
    //评价收藏总数
    public static final String REVIEW_STAR_COUNT_KEY = InteractionRedisKeyConstants.Star.REVIEW_STAR_COUNT;
    //评价收藏脏数据列表
    public static final String REVIEW_STAR_DIRTY_KEY = InteractionRedisKeyConstants.Star.REVIEW_STAR_DIRTY;

    /**
     * 评论数据
     */
    //博客的评论
    public static final String BLOG_COMMENT_NEW_RANK_KEY = InteractionRedisKeyConstants.Comment.BLOG_COMMENT_NEW_RANK;
    public static final String BLOG_COMMENT_HOT_RANK_KEY = InteractionRedisKeyConstants.Comment.BLOG_COMMENT_HOT_RANK;
    //博客评论总数
    public static final String BLOG_COMMENT_COUNT_KEY = InteractionRedisKeyConstants.Comment.BLOG_COMMENT_COUNT;
    //博客评论脏数据列表
    public static final String BLOG_COMMENT_SYNC_KEY = InteractionRedisKeyConstants.Comment.BLOG_COMMENT_SYNC;
    public static final String BLOG_COMMENT_CALC_KEY = InteractionRedisKeyConstants.Comment.BLOG_COMMENT_CALC;

    //评价的评论
    public static final String REVIEW_COMMENT_NEW_RANK_KEY = InteractionRedisKeyConstants.Comment.REVIEW_COMMENT_NEW_RANK;
    public static final String REVIEW_COMMENT_HOT_RANK_KEY = InteractionRedisKeyConstants.Comment.REVIEW_COMMENT_HOT_RANK;
    //评价的评论总数
    public static final String REVIEW_COMMENT_COUNT_KEY = InteractionRedisKeyConstants.Comment.REVIEW_COMMENT_COUNT;
    //评价的评论脏数据列表
    public static final String REVIEW_COMMENT_SYNC_KEY = InteractionRedisKeyConstants.Comment.REVIEW_COMMENT_SYNC;
    public static final String REVIEW_COMMENT_CALC_KEY = InteractionRedisKeyConstants.Comment.REVIEW_COMMENT_CALC;

    //评论的评论
    public static final String COMMENT_COMMENT_NEW_RANK_KEY = InteractionRedisKeyConstants.Comment.COMMENT_COMMENT_NEW_RANK;
    public static final String COMMENT_COMMENT_HOT_RANK_KEY = InteractionRedisKeyConstants.Comment.COMMENT_COMMENT_HOT_RANK;
    //评论的评论总数
    public static final String COMMENT_COMMENT_COUNT_KEY = InteractionRedisKeyConstants.Comment.COMMENT_COMMENT_COUNT;
    //评论的评论脏数据列表
    public static final String COMMENT_COMMENT_SYNC_KEY = InteractionRedisKeyConstants.Comment.COMMENT_COMMENT_SYNC;
    public static final String COMMENT_COMMENT_CALC_KEY = InteractionRedisKeyConstants.Comment.COMMENT_COMMENT_CALC;
    //用户评论反向索引
    public static final String USER_COMMENT_KEY = InteractionRedisKeyConstants.Comment.USER_COMMENT;

    /**
     * 评价数据
     */
    //店铺评价
    public static final String SHOP_REVIEW_HOT_RANK_KEY = InteractionRedisKeyConstants.Review.SHOP_REVIEW_HOT_RANK;
    public static final String SHOP_REVIEW_NEW_RANK_KEY = InteractionRedisKeyConstants.Review.SHOP_REVIEW_NEW_RANK;
    //店铺评价总数
    public static final String SHOP_REVIEW_COUNT_KEY = InteractionRedisKeyConstants.Review.SHOP_REVIEW_COUNT;
    //店铺评价脏数据列表
    public static final String SHOP_REVIEW_SYNC_KEY = InteractionRedisKeyConstants.Review.SHOP_REVIEW_SYNC;
    public static final String SHOP_REVIEW_CALC_KEY = InteractionRedisKeyConstants.Review.SHOP_REVIEW_CALC;

    //商品评价
    public static final String PRODUCT_REVIEW_HOT_RANK_KEY = InteractionRedisKeyConstants.Review.PRODUCT_REVIEW_HOT_RANK;
    public static final String PRODUCT_REVIEW_NEW_RANK_KEY = InteractionRedisKeyConstants.Review.PRODUCT_REVIEW_NEW_RANK;
    //商品评价总数
    public static final String PRODUCT_REVIEW_COUNT_KEY = InteractionRedisKeyConstants.Review.PRODUCT_REVIEW_COUNT;
    //商品评价脏数据列表
    public static final String PRODUCT_REVIEW_SYNC_KEY = InteractionRedisKeyConstants.Review.PRODUCT_REVIEW_SYNC;
    public static final String PRODUCT_REVIEW_CALC_KEY = InteractionRedisKeyConstants.Review.PRODUCT_REVIEW_CALC;
    //用户评价反向索引
    public static final String USER_REVIEW_KEY = InteractionRedisKeyConstants.Review.USER_REVIEW;
    /**
     * 商店地理位置
     */
    public static final String SHOP_GEO_KEY = "shop:geo:";

    /**
     * 缓存AI评论
     */
    public static final String CACHE_AI_REVIEW_KEY = "cache:aiReview:";
    public static final long CACHE_AI_REVIEW_TTL = 600L;
    /**
     * 用户的首页搜索历史
     */
    public static final String SEARCH_INDEX_HISTORY_KEY = "search:index:history:";

    /**
     * 用户的个人中心搜索历史
     */
    public static final String SEARCH_USER_HISTORY_KEY = "search:user:history:";
    public static final Integer SEARCH_HISTORY_TTL = 30;//30天
    /**
     * 热门搜索
     */
    public static final String SEARCH_HOT_KEYWORDS = "search:hot:keywords";
    public static final Integer SEARCH_HOT_TTL = 24;//24小时
    /**
     * 用户在线状态
     */
    public static final String IM_ONLINE_KEY = "im:online:";
    /**
     * 用户会话
     */
    public static final String IM_SESSION_KEY = "im:session:";

    /** 实体的自身热度榜及独立计算队列 */
    public static final String BLOG_CALC_QUEUE_KEY = "queue:calc:blog";
    public static final String SHOP_CALC_QUEUE_KEY = "queue:calc:shop";
    public static final String PRODUCT_CALC_QUEUE_KEY = "queue:calc:product";
    public static final String BLOG_HOT_RANK_KEY = "blog:hot:rank:";
    public static final String SHOP_HOT_RANK_KEY = "shop:hot:rank:";
    public static final String PRODUCT_HOT_RANK_KEY = "product:hot:rank:";
}
