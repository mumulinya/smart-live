package com.smartLive.common.core.constant;

public class MqConstants {
    // 订单交换机名称
    public static final String  ORDER_EXCHANGE_NAME = "order.direct";
    public static final String  ORDER_SECKILL_QUEUE = "order.seckill.queue";
    public static final String  ORDER_SECKILL_ROUTING = "order.seckill.voucher";
    public static final String  ORDER_BUY_QUEUE = "order.buy.queue";
    public static final String  ORDER_BUY_ROUTING = "order.buy.voucher";

    //博客交换机
    public static final String  BLOG_EXCHANGE_NAME = "blog.direct";
    public static final String  BLOG_FEED_QUEUE = "blog.feed.queue";
    public static final String  BLOG_FEED_ROUTING = "blog.feed.user";
    public static final String  BLOG_COMMENT_QUEUE = "blog.comment.queue";
    public static final String  BLOG_COMMENT_ROUTING = "blog.comment.add";
    //店铺交换机
    public static final String  SHOP_EXCHANGE_NAME = "shop.direct";
    public static final String  SHOP_COMMENT_QUEUE = "shop.comment.queue";
    public static final String  SHOP_COMMENT_ROUTING = "shop.comment.add";
    public static final String  SHOP_FOLLOW_QUEUE = "shop.follow.queue";
    public static final String  SHOP_FOLLOW_ROUTING = "shop.follow.add";
    //用户交换机
    public static final String  USER_EXCHANGE_NAME = "user.direct";
    public static final String  USER_FOLLOW_QUEUE = "user.follow.queue";
    public static final String  USER_FOLLOW_ROUTING = "user.follow.add";

}
