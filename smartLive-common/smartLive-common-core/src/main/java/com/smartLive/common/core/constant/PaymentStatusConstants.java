package com.smartLive.common.core.constant;

/**
 * 支付状态常量。
 */
public class PaymentStatusConstants {

    /** 待支付 */
    public static final int PENDING = 0;

    /** 支付成功 */
    public static final int SUCCESS = 1;

    /** 支付失败 */
    public static final int FAILED = 2;

    /** 已取消/已关闭 */
    public static final int CANCELED = 3;

    private PaymentStatusConstants() {
    }
}
