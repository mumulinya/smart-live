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
     * 逻辑过期缓存的物理过期时间
     */
    public static final Long CACHE_LOGICAL_EXPIRE_TTL = 1L;
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
    /** 秒杀预热窗口：活动开始前多少小时进行预热 */
    public static final long SECKILL_PRE_HEAT_WINDOW_HOURS = 2L;
    /** 秒杀临期提醒窗口：活动结束前多少分钟触发通知 */
    public static final long SECKILL_SOON_END_WINDOW_MINUTES = 30L;
    /** 秒杀临期通知防重 Key 前缀 */
    public static final String SECKILL_NOTIFY_KEY = "seckill:notify:";
    /**
     * 博客缓存
     */
    public static final String CACHE_BLOG_KEY = "cache:blog:";
    public static final String LOCK_BLOG_KEY = "lock:blog:";
    public static final Long CACHE_BLOG_TTL = 30L;
    public static final String CACHE_HOT_BLOG_KEY = "cache:hotBlog:";
    public static final Long CACHE_HOT_BLOG_TTL = 1L;//1天
    /**
     * 博客类型
     */
    public static final String CACHE_BLOG_TYPE_KEY = "cache:blogType:";

    /**
     * 商品缓存
     */
    public static final String CACHE_PRODUCT_KEY = "cache:product:";
    public static final String LOCK_PRODUCT_KEY = "lock:product";
    public static final Long CACHE_PRODUCT_TTL = 30L;

    /**
     * 用户缓存
     */
    public static final String CACHE_USER_KEY = "cache:user:";
    public static final String LOCK_USER_KEY = "lock:user";
    public static final Long CACHE_USER_TTL = 30L;

    /**
     * 评价缓存
     */
    public static final String CACHE_REVIEW_KEY = "cache:review:";
    public static final String LOCK_REVIEW_KEY = "lock:review";
    public static final Long CACHE_REVIEW_TTL = 30L;
    public static final String CACHE_COMMENT_KEY = "cache:comment:";
    public static final Long CACHE_COMMENT_TTL = 30L;
    public static final String LOCK_COMMENT_KEY = "lock:comment:";


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
