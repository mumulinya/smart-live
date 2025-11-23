package com.smartLive.index.service;

import com.smartLive.index.domain.*;

import java.util.List;

/**
 * 首页模块 Service 接口（完全匹配前端数据需求）
 */
public interface IIndexService {

    /**
     * 获取首页统计数据（totalShops/totalBlogs等）
     */
    IndexStatsVO getIndexStats();

    /**
     * 获取店铺统计图表数据
     * @param type 统计类型: week(本周)/month(本月)
     */
    ShopStatsChartVO getShopStatsChart(String type);

    /**
     * 获取订单统计图表数据
     * @param type 统计类型: week(本周)/month(本月)
     */
    OrderStatsChartVO getOrderStatsChart(String type);

    /**
     * 获取代金券统计图表数据
     */
    CouponStatsChartVO getCouponStatsChart();

    /**
     * 获取最新店铺列表
     * @param limit 返回数量
     */
    List<RecentShopVO> getRecentShops(Integer limit);

    /**
     * 获取最新订单列表
     * @param limit 返回数量
     */
    List<RecentOrderVO> getRecentOrders(Integer limit);

    /**
     * 获取最新博客列表
     * @param limit 返回数量
     */
    List<RecentBlogVO> getRecentBlogs(Integer limit);
}