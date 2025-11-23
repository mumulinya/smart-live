package com.smartLive.index.domain;

import lombok.Data;

@Data
public class CouponStatsChartVO {
    private Integer used;       // 已使用数量
    private Integer unused;     // 未使用数量
    private Integer expired;    // 已过期数量
}