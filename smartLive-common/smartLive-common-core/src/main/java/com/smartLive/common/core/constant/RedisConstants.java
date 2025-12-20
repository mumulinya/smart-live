package com.smartLive.common.core.constant;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 30L;

    public static final Long CACHE_NULL_TTL = 2L;

    public static final Long CACHE_SHOP_TTL = 30L;
    public static final String CACHE_SHOP_KEY = "cache:shop:";
    public static final String CACHE_SHOP_lIST_KEY = "cache:shopList:";

    public static final String CACHE_SHOP_TYPE_KEY = "cache:shopType";
    public static final Long CACHE_SHOP_TYPE_TTL = 30L;

    public static final String LOCK_SHOP_KEY = "lock:shop:";

    public static final Long LOCK_SHOP_TTL = 10L;

    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
//    public static final String BLOG_LIKED_KEY = "blog:liked:";
    public static final String CACHE_BLOG_KEY = "cache:blog:";
    public static final String CACHE_HOT_BLOG_KEY = "cache:hotBlog:";
    public static final Integer CACHE_HOT_BLOG_TTL = 1;//1天

    public static final String CACHE_BLOG_TYPE_KEY = "cache:blogType:";
    /**
     * 关注
     */
    public static final String FOLLOW_USER_KEY = "follow:user:";
    public static final String FANS_USER_KEY = "fans:user:";
    public static final String FOLLOW_SHOP_KEY = "follow:shop:";
    public static final String FANS_SHOP_KEY = "fans:shop:";
    /**
     * 推送新闻
     */
    public static final String FEED_KEY = "feed:";
    /**
     * 推送数据
     */
    //博文推送
    public static final String BLOG_FEED_KEY = "blog:feed:";
    //秒杀代金券推送
    public static final String SECKILL_VOUCHER_FEED_KEY = "seckill:voucher:feed:";
    //优惠券推送
    public static final String VOUCHER_FEED_KEY = "voucher:feed:";

    /**
     *  点赞数据
     */
    //博文点赞
    public static final String BLOG_LIKED_KEY = "BLOG:liked:";
    //评论点赞
    public static final String COMMENT_LIKED_KEY = "comment:liked:";
    /**
     * 收藏数据
     */
    //博文收藏
    public static final String BLOG_COLLECT_KEY = "blog:collect:";
    //代金券收藏
    public static final String VOUCHER_COLLECT_KEY = "voucher:collect:";
    //评论收藏
    public static final String COMMENT_COLLECT_KEY = "comment:collect:";
    /**
     * 评论数据
     */
    //博客的评论
    public static final String BLOG_COMMENT_KEY = "blog:comment:";
    //代金券的评论
    public static final String VOUCHER_COMMENT_KEY = "voucher:comment:";
    //店铺的评论
    public static final String SHOP_COMMENT_KEY = "shop:comment:";
    //评论的评论
    public static final String COMMENT_COMMENT_KEY = "comment:comment:";
    /**
     * 商店地理位置
     */
    public static final String SHOP_GEO_KEY = "shop:geo:";
    public static final String USER_SIGN_KEY = "sign:";

    public static final String CACHE_COMMENT_KEY = "cache:comment:";
    /**
     * 缓存AI评论
     */
    public static final String CACHE_AI_COMMENT_KEY = "cache:aiComment:";
    public static final long CACHE_AI_COMMENT_TTL = 600L;
   /**
    * 搜索历史
    */
    public static final String SEARCH_HISTORY_KEY = "search:history:";
    public static final Integer SEARCH_HISTORY_TTL = 30;//30天
    /**
     * 热门搜索
     */
    public static final String SEARCH_HOT_KEYWORDS = "search:hot:keywords";
    public static final Integer SEARCH_HOT_TTL = 24;//24小时
}
