package com.smartLive.index.domain;

import lombok.Data;

import java.util.List;

/**
 * 订单统计图表 VO（匹配前端 orderChart）
 */
@Data
public class OrderStatsChartVO {
    private List<String> dates;        // 日期数组
    private List<Integer> orders;      // 订单数量
    private List<Integer> revenue;     // 营收金额
}