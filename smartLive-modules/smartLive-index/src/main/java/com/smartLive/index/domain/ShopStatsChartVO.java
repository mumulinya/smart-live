package com.smartLive.index.domain;

import lombok.Data;

import java.util.List;

/**
 * 店铺统计图表 VO（匹配前端 shopChart）
 */
@Data
public class ShopStatsChartVO {
    private List<String> dates;        // 日期数组（如 ['01-08', '01-09']）
    private List<Integer> newShops;    // 新增店铺数
    private List<Integer> approvedShops; // 审核通过店铺数
}