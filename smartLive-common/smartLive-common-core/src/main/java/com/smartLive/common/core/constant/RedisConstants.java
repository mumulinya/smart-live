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
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    public static final String CACHE_BLOG_KEY = "cache:blog:";
    public static final String CACHE_HOT_BLOG_KEY = "cache:hotBlog:";
    public static final String FOLLOW_USER_KEY = "follow:user:";

    public static final String FOLLOW_SHOP_KEY = "follow:shop:";
    /**
     * 推送新闻
     */
    public static final String FEED_KEY = "feed:";
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
