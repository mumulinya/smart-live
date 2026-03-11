package com.smartLive.common.core.constant;

/**
 * MQ 消费幂等 Redis Key 常量
 *
 * @author smartLive
 */
public class RedisMqIdempotentConstants {

    /** 幂等 key 统一前缀 */
    public static final String MQ_IDEMPOTENT_PREFIX = "mq:idempotent:";

    /** 审核模块前缀 */
    public static final String AUDIT_PREFIX = MQ_IDEMPOTENT_PREFIX + "audit:";

    /** ES 搜索同步前缀 */
    public static final String SEARCH_PREFIX = MQ_IDEMPOTENT_PREFIX + "search:";

    /** Milvus 向量同步前缀 */
    public static final String MILVUS_PREFIX = MQ_IDEMPOTENT_PREFIX + "milvus:";

    /** AI 模块前缀 */
    public static final String AI_PREFIX = MQ_IDEMPOTENT_PREFIX + "ai:";

    /** 关注/Feed 模块前缀 */
    public static final String FOLLOW_PREFIX = MQ_IDEMPOTENT_PREFIX + "follow:";

    /** 支付模块前缀 */
    public static final String PAY_PREFIX = MQ_IDEMPOTENT_PREFIX + "pay:";

    /** 订单模块前缀 */
    public static final String ORDER_PREFIX = MQ_IDEMPOTENT_PREFIX + "order:";

    /** 私聊模块前缀 */
    public static final String CHAT_PREFIX = MQ_IDEMPOTENT_PREFIX + "chat:";

    /** IM 推送模块前缀 */
    public static final String IM_PREFIX = MQ_IDEMPOTENT_PREFIX + "im:";

    /** 商品模块前缀 */
    public static final String PRODUCT_PREFIX = MQ_IDEMPOTENT_PREFIX + "product:";

    /** 积分模块前缀 */
    public static final String POINTS_PREFIX = MQ_IDEMPOTENT_PREFIX + "points:";

    /** 默认幂等 key 过期时间（秒）：24小时 */
    public static final long DEFAULT_TTL_SECONDS = 86400L;

    private RedisMqIdempotentConstants() {
    }
}
