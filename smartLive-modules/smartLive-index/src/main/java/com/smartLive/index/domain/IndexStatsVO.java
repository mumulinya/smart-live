package com.smartLive.index.domain;

import lombok.Data;

@Data
public class IndexStatsVO {
    private Integer totalShops;    // 总店铺数
    private Integer totalBlogs;    // 总博客数
    private Integer totalCoupons;  // 总代金券数
    private Integer totalOrders;   // 今日订单数
    private Integer todayReviews;  // 今日评论数（前端data中定义，需兼容）
}