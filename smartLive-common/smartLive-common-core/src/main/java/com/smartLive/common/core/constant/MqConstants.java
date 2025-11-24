package com.smartLive.common.core.constant;

public class MqConstants {
    // 订单交换机名称
    public static final String  ORDER_EXCHANGE_NAME = "order.direct";
    public static final String  ORDER_SECKILL_QUEUE = "order.seckill.queue";
    public static final String  ORDER_SECKILL_ROUTING = "order.seckill.voucher";
    public static final String  ORDER_BUY_QUEUE = "order.buy.queue";
    public static final String  ORDER_BUY_ROUTING = "order.buy.voucher";
    //订单死信交换机
    public static final String  ORDER_DEAD_LETTER_EXCHANGE_NAME = "order.dead.letter.direct";
    public static final String  ORDER_DEAD_LETTER_QUEUE = "order.dead.letter.queue";
    public static final String  ORDER_DEAD_LETTER_ROUTING = "order.dead.letter.routing";


    //支付延迟交换机
    public static final String  ORDER_DELAY_EXCHANGE_NAME = "order.delay.direct";
    public static final String  ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String  ORDER_DELAY_ROUTING = "order.delay.routing";
    public static final Integer DELAY_TIME = 15 * 60000; // 15分钟 = 900000 毫秒
    // 博客交换机
    public static final String  BLOG_EXCHANGE_NAME = "blog.direct";
    public static final String  BLOG_FEED_QUEUE = "blog.feed.queue";
    public static final String  BLOG_FEED_ROUTING = "blog.feed.user";
    public static final String  BLOG_COMMENT_QUEUE = "blog.comment.queue";
    public static final String  BLOG_COMMENT_ROUTING = "blog.comment.add";
    //博客死信交换机
    public static final String  BLOG_DEAD_LETTER_EXCHANGE_NAME = "blog.dead.letter.direct";
    public static final String  BLOG_DEAD_LETTER_QUEUE = "blog.dead.letter.queue";
    public static final String  BLOG_DEAD_LETTER_ROUTING = "blog.dead.letter.routing";



    //店铺交换机
    public static final String  SHOP_EXCHANGE_NAME = "shop.direct";
    public static final String  SHOP_COMMENT_QUEUE = "shop.comment.queue";
    public static final String  SHOP_COMMENT_ROUTING = "shop.comment.add";
    public static final String  SHOP_FOLLOW_QUEUE = "shop.follow.queue";
    public static final String  SHOP_FOLLOW_ROUTING = "shop.follow.add";
    //店铺死信交换机
    public static final String  SHOP_DEAD_LETTER_EXCHANGE_NAME = "shop.dead.letter.direct";
    public static final String  SHOP_DEAD_LETTER_QUEUE = "shop.dead.letter.queue";
    public static final String  SHOP_DEAD_LETTER_ROUTING = "shop.dead.letter.routing";



    //用户交换机
    public static final String  USER_EXCHANGE_NAME = "user.direct";
    public static final String  USER_FOLLOW_QUEUE = "user.follow.queue";
    public static final String  USER_FOLLOW_ROUTING = "user.follow.add";

    //ai交换机
    public static final String AI_EXCHANGE_NAME = "ai.direct";
    public static final String AI_COMMENT_QUEUE = "ai.comment.queue";
    public static final String AI_COMMENT_ROUTING = "ai.comment.create";
    //私聊交换机
    public static final String CHAT_EXCHANGE_NAME = "chat.topic";
    public static final String CHAT_MESSAGE_QUEUE = "chat.message.queue";
    public static final String CHAT_MESSAGE_ROUTING = "chat.session.";
    //死信交换机
    public static final String DEAD_LETTER_EXCHANGE_NAME = "dead.letter.direct";
    public static final String DEAD_LETTER_QUEUE = "dead.letter.queue";
    public static final String DEAD_LETTER_ROUTING = "dead.letter.routing";

    // ==================== ES 交换机 ====================
    public static final String ES_EXCHANGE = "es.sync.exchange";

    // ==================== ES 队列名称 ====================
    public static final String ES_INSERT_QUEUE = "es.sync.insert.queue";
    public static final String ES_BATCH_INSERT_QUEUE = "es.sync.batch.insert.queue";
    public static final String ES_DELETE_QUEUE = "es.sync.delete.queue";

    // ==================== Milvus 交换机 ====================
    public static final String MILVUS_EXCHANGE = "milvus.sync.exchange";

    //数据同步死信交换机
    //店铺死信交换机
    public static final String  DATA_DEAD_LETTER_EXCHANGE_NAME = "data.dead.letter.direct";
    public static final String  DATA_DEAD_LETTER_QUEUE = "data.dead.letter.queue";
    public static final String  DATA_DEAD_LETTER_ES = "data.dead.letter.es";
    public static final String  DATA_DEAD_LETTER_MILVUS = "data.dead.letter.milvus";
    // ==================== Milvus 队列名称 ====================
    public static final String MILVUS_INSERT_QUEUE = "milvus.sync.insert.queue";
    public static final String MILVUS_BATCH_INSERT_QUEUE = "milvus.sync.batch.insert.queue";
    public static final String MILVUS_DELETE_QUEUE = "milvus.sync.delete.queue";

    // ==================== ES 路由键 ====================
    public static final String ES_ROUTING_VOUCHER_INSERT = "es.voucher.insert";
    public static final String ES_ROUTING_USER_INSERT = "es.user.insert";
    public static final String ES_ROUTING_SHOP_INSERT = "es.shop.insert";
    public static final String ES_ROUTING_BLOG_INSERT = "es.blog.insert";

    public static final String ES_ROUTING_VOUCHER_BATCH_INSERT = "es.voucher.batch.insert";
    public static final String ES_ROUTING_USER_BATCH_INSERT = "es.user.batch.insert";
    public static final String ES_ROUTING_SHOP_BATCH_INSERT = "es.shop.batch.insert";
    public static final String ES_ROUTING_BLOG_BATCH_INSERT = "es.blog.batch.insert";

    public static final String ES_ROUTING_VOUCHER_DELETE = "es.voucher.delete";
    public static final String ES_ROUTING_USER_DELETE = "es.user.delete";
    public static final String ES_ROUTING_SHOP_DELETE = "es.shop.delete";
    public static final String ES_ROUTING_BLOG_DELETE = "es.blog.delete";

    // ==================== Milvus 路由键 ====================
    public static final String MILVUS_ROUTING_VOUCHER_INSERT = "milvus.voucher.insert";
    public static final String MILVUS_ROUTING_USER_INSERT = "milvus.user.insert";
    public static final String MILVUS_ROUTING_SHOP_INSERT = "milvus.shop.insert";
    public static final String MILVUS_ROUTING_BLOG_INSERT = "milvus.blog.insert";

    public static final String MILVUS_ROUTING_VOUCHER_BATCH_INSERT = "milvus.voucher.batch.insert";
    public static final String MILVUS_ROUTING_USER_BATCH_INSERT = "milvus.user.batch.insert";
    public static final String MILVUS_ROUTING_SHOP_BATCH_INSERT = "milvus.shop.batch.insert";
    public static final String MILVUS_ROUTING_BLOG_BATCH_INSERT = "milvus.blog.batch.insert";

    public static final String MILVUS_ROUTING_VOUCHER_DELETE = "milvus.voucher.delete";
    public static final String MILVUS_ROUTING_USER_DELETE = "milvus.user.delete";
    public static final String MILVUS_ROUTING_SHOP_DELETE = "milvus.shop.delete";
    public static final String MILVUS_ROUTING_BLOG_DELETE = "milvus.blog.delete";
}
