package com.smartLive.index.domain;

import lombok.Data;

/**
 * 最新订单 VO（匹配前端 recentOrders 单个对象）
 */
@Data
public class RecentOrderVO {
    private Long id;            // 订单ID
    private String orderNo;     // 订单号
    private String shopName;    // 店铺名称
    private Integer amount;     // 金额
    private Integer status;     // 状态：1待支付/2待使用/3已完成/4已退款/5已取消
    private String createTime;  // 创建时间（格式：YYYY-MM-DD HH:mm:ss）
}