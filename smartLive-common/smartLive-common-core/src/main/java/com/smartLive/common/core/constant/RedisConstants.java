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
    public static final String FOLLOW_USER_KEY = "follow:user:";
    //用户粉丝
    public static final String FANS_USER_KEY = "fans:user:";
    //店铺关注
    public static final String FOLLOW_SHOP_KEY = "follow:shop:";
    //店铺粉丝
    public static final String FANS_SHOP_KEY = "fans:shop:";
    //商品关注
    public static final String FOLLOW_PRODUCT_KEY = "follow:product:";
    //商品粉丝
    public static final String FANS_PRODUCT_KEY = "fans:product:";
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
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    //博客点赞总数
    public static final String BLOG_LIKED_COUNT_KEY = "blog:liked:count:";
    //博客点赞脏数据列表
    public static final String BLOG_LIKED_DIRTY_KEY = "blog:liked:dirty:";

    //评论点赞
    public static final String COMMENT_LIKED_KEY = "comment:liked:";
    //评论点赞总数
    public static final String COMMENT_LIKED_COUNT_KEY = "comment:liked:count:";
    //评论点赞脏数据列表
    public static final String COMMENT_LIKED_DIRTY_KEY = "comment:liked:dirty";

    //评价点赞
    public static final String REVIEW_LIKED_KEY = "review:liked:";
    //评价点赞总数
    public static final String REVIEW_LIKED_COUNT_KEY = "review:liked:count:";
    //评价点赞脏数据列表
    public static final String REVIEW_LIKED_DIRTY_KEY = "review:liked:dirty:";
    /**
     * 收藏数据
     */
    //博文收藏
    public static final String BLOG_STAR_KEY = "blog:star:";
    //博客收藏总数
    public static final String BLOG_STAR_COUNT_KEY = "blog:star:count:";
    //博客收藏脏数据列表
    public static final String BLOG_STAR_DIRTY_KEY = "blog:star:dirty:";

    //店铺收藏
    public static final String SHOP_STAR_KEY = "shop:star:";
    //店铺收藏总数
    public static final String SHOP_STAR_COUNT_KEY = "shop:star:count:";
    //店铺收藏脏数据列表
    public static final String SHOP_STAR_DIRTY_KEY = "shop:star:dirty:";

    //商品收藏
    public static final String PRODUCT_STAR_KEY = "product:star:";
    //商品收藏总数
    public static final String PRODUCT_STAR_COUNT_KEY = "product:star:count:";
    //商品收藏脏数据列表
    public static final String PRODUCT_STAR_DIRTY_KEY = "product:star:dirty:";

    //评价收藏
    public static final String REVIEW_STAR_KEY = "review:star:";
    //评价收藏总数
    public static final String REVIEW_STAR_COUNT_KEY = "review:star:count:";
    //评价收藏脏数据列表
    public static final String REVIEW_STAR_DIRTY_KEY = "review:star:dirty:";

    /**
     * 评论数据
     */
    //博客的评论
    public static final String BLOG_COMMENT_KEY = "blog:comment:";
    //博客评论总数
    public static final String BLOG_COMMENT_COUNT_KEY = "blog:comment:count:";
    //博客评论脏数据列表
    public static final String BLOG_COMMENT_DIRTY_KEY = "blog:comment:dirty:";

    //评价的评论
    public static final String REVIEW_COMMENT_KEY = "review:comment:";
    //评价的评论总数
    public static final String REVIEW_COMMENT_COUNT_KEY = "review:comment:count:";
    //评价的评论脏数据列表
    public static final String REVIEW_COMMENT_DIRTY_KEY = "review:comment:dirty:";

    //评论的评论
    public static final String COMMENT_COMMENT_KEY = "comment:comment:";
    //评论的评论总数
    public static final String COMMENT_COMMENT_COUNT_KEY = "comment:comment:count:";
    //评论的评论脏数据列表
    public static final String COMMENT_COMMENT_DIRTY_KEY = "comment:comment:dirty:";

    /**
     * 评价数据
     */
    //店铺评价
    public static final String SHOP_REVIEW_KEY = "shop:review:";
    //店铺评价总数
    public static final String SHOP_REVIEW_COUNT_KEY = "shop:review:count:";
    //店铺评价脏数据列表
    public static final String SHOP_REVIEW_DIRTY_KEY = "shop:review:dirty:";

    //商品评价
    public static final String PRODUCT_REVIEW_KEY = "product:review:";
    //商品评价总数
    public static final String PRODUCT_REVIEW_COUNT_KEY = "product:review:count:";
    //商品评价脏数据列表
    public static final String PRODUCT_REVIEW_DIRTY_KEY = "product:review:dirty:";
    /**
     * 商店地理位置
     */
    public static final String SHOP_GEO_KEY = "shop:geo:";

    /**
     * 缓存AI评论
     */
    public static final String CACHE_AI_COMMENT_KEY = "cache:aiComment:";
    public static final long CACHE_AI_COMMENT_TTL = 600L;
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
}
