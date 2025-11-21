package com.smartLive.common.core.constant;

/**
 * 缓存常量信息
 * 
 * @author smartLive
 */
public class CacheConstants
{
    /**
     * 缓存有效期，默认720（分钟）
     */
    public final static long EXPIRATION = 720;

    /**
     * 缓存刷新时间，默认120（分钟）
     */
    public final static long REFRESH_TIME = 120;

    /**
     * 密码最大错误次数
     */
    public final static int PASSWORD_MAX_RETRY_COUNT = 5;

    /**
     * 密码锁定时间，默认10（分钟）
     */
    public final static long PASSWORD_LOCK_TIME = 10;

    /**
     * 权限缓存前缀
     */
    public final static String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 验证码 redis key
     */
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * 参数管理 cache key
     */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache key
     */
    public static final String SYS_DICT_KEY = "sys_dict:";

    /**
     * 登录账户密码错误次数 redis key
     */
    public static final String PWD_ERR_CNT_KEY = "pwd_err_cnt:";

    /**
     * 登录IP黑名单 cache key
     */
    public static final String SYS_LOGIN_BLACKIPLIST = SYS_CONFIG_KEY + "sys.login.blackIPList";

    /**
     * 登录验证码 redis key
     */
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    /**
     * 登录用户 redis key
     * */
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 30L;
    /**
     * 缓存空值
     */
    public static final Long CACHE_NULL_TTL = 2L;
    /**
     * 缓存商店信息
     */
    public static final String CACHE_SHOP_KEY = "cache:shop:";
    public static final String CACHE_SHOP_lIST_KEY = "cache:shopList:";
    public static final Long CACHE_SHOP_TTL = 30L;
    /**
     * 缓存商店类型信息
     */
    public static final String CACHE_SHOP_TYPE_KEY = "cache:shopType";
    public static final Long CACHE_SHOP_TYPE_TTL = 30L;
    /**
     * 缓存锁
     *  */
    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;
    /**
     * 秒杀商品库存
     */
    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    /**
     * 博客点赞
     */
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    /**
     * 缓存博客信息
     */
    public static final String CACHE_BLOG_KEY = "cache:blog:";
    /**
     * 缓存热门博客信息
     */
    public static final Integer CACHE_HOT_BLOG_TTL = 1;//1天

    /**
     * 博客类型
     */
    public static final String CACHE_BLOG_TYPE_KEY = "cache:blogType:";
    /**
     * 关注用户
     */

    public static final String FOLLOW_SHOP_KEY = "follow:shop:";
    /**
     * 推送博客
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
