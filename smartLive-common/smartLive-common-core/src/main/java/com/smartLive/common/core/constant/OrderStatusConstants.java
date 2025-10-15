package com.smartLive.common.core.constant;

import java.util.HashMap;
import java.util.Map;

//订单状态
public class OrderStatusConstants {
    //未支付
    public static final int UNPAID = 1;
    //已支付
    public static final int PAID = 2;
    //已核销
    public static final int VERIFIED = 3;
    //已取消
    public static final int CANCELLED = 4;
    //退款中
    public static final int REFUNDING = 5;
    //已退款
    public static final int REFUNDED = 6;
}