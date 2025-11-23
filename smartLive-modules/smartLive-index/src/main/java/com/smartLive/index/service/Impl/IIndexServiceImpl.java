package com.smartLive.index.service.Impl;

import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.comment.api.RemoteCommentService;
import com.smartLive.index.domain.*;
import com.smartLive.index.service.IIndexService;
import com.smartLive.marketing.api.RemoteMarketingService;
import com.smartLive.order.api.RemoteOrderService;
import com.smartLive.order.api.dto.VoucherOrderDTO;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.domain.ShopDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * 首页模块 Service 实现类
 * 注：以下包含「模拟数据」和「真实查询占位」，可根据数据库实际情况替换
 */
@Service
@Slf4j
public class IIndexServiceImpl implements IIndexService {

    @Autowired
    private ExecutorService executorService;

    @Autowired
    private RemoteBlogService remoteBlogService;

    @Autowired
    private RemoteShopService remoteShopService;

    @Autowired
    private RemoteOrderService remoteOrderService;

    @Autowired
    private RemoteMarketingService remoteMarketService;

    @Autowired
    private RemoteCommentService remoteCommentService;




    /**
     * 获取首页统计数据
     */
    @Override
    public IndexStatsVO getIndexStats() {
        CountDownLatch latch=new CountDownLatch(5);
        // 方式1：真实查询（推荐）
        // return indexStatsMapper.selectIndexStats();
        //使用线程池查询博客总数
        Future<Integer> blogCountFuture = executorService.submit(() -> {
            log.info("线程：{}开始查询博客总数",Thread.currentThread().getName());
            Integer blogCount = remoteBlogService.getBlogTotal().getData();
            latch.countDown();
            return blogCount;
        });
        //使用线程池查询店铺总数
        Future<Integer> shopCountFuture = executorService.submit(() -> {
            log.info("线程：{}开始查询店铺总数",Thread.currentThread().getName());
            Integer shopCount = remoteShopService.getShopTotal().getData();
            latch.countDown();
            return shopCount;
        });
        //使用线程池查询代金券总数
        Future<Integer> couponCountFuture = executorService.submit(() -> {
            log.info("线程：{}开始查询代金券总数",Thread.currentThread().getName());
                    Integer couponCount = remoteMarketService.getCouponTotal().getData();
                    latch.countDown();
                    return couponCount;
                });
        //使用线程池查询订单总数
        Future<Integer> orderCountFuture = executorService.submit(() -> {
            log.info("线程：{}开始查询订单总数",Thread.currentThread().getName());
            Integer orderCount = remoteOrderService.getOrderTotal().getData();
            latch.countDown();
            return orderCount;
        });
        //使用线程池查询评论总数
        Future<Integer> commentCountFuture = executorService.submit(() -> {
            log.info("线程：{}开始查询评论总数",Thread.currentThread().getName());
            Integer commentCount = remoteCommentService.getCommentTotal().getData();
            latch.countDown();
            return commentCount;
        });
       try {
           log.info("开始查询数据");
           latch.await();
           log.info("查询数据结束");
           IndexStatsVO stats = new IndexStatsVO();
           stats.setTotalShops(shopCountFuture.get());
           stats.setTotalBlogs(blogCountFuture.get());
           stats.setTotalCoupons(couponCountFuture.get());
           stats.setTotalOrders(orderCountFuture.get());
           stats.setTodayReviews(commentCountFuture.get());
           return stats;
       }catch (Exception e) {
           e.printStackTrace();
           return null;
       }
    }

    /**
     * 获取店铺统计图表数据
     */
    @Override
    public ShopStatsChartVO getShopStatsChart(String type) {
        // 方式1：真实查询
        // return shopStatsChartMapper.selectShopStatsChart(type);
        
        // 方式2：模拟数据（匹配前端mockData）
        ShopStatsChartVO chart = new ShopStatsChartVO();
        if ("week".equals(type)) {
            chart.setDates(Arrays.asList("01-08", "01-09", "01-10", "01-11", "01-12", "01-13", "01-14"));
            chart.setNewShops(Arrays.asList(5, 8, 12, 6, 15, 9, 11));
            chart.setApprovedShops(Arrays.asList(4, 7, 10, 5, 13, 8, 10));
        } else if ("month".equals(type)) {
            chart.setDates(Arrays.asList("12-15", "12-20", "12-25", "12-30", "01-04", "01-09", "01-14"));
            chart.setNewShops(Arrays.asList(8, 12, 6, 15, 9, 11, 7));
            chart.setApprovedShops(Arrays.asList(7, 10, 5, 13, 8, 10, 6));
        }
        return chart;
    }

    /**
     * 获取订单统计图表数据
     */
    @Override
    public OrderStatsChartVO getOrderStatsChart(String type) {
        // 方式1：真实查询
        // return orderStatsChartMapper.selectOrderStatsChart(type);
        
        // 方式2：模拟数据（匹配前端mockData）
        OrderStatsChartVO chart = new OrderStatsChartVO();
        if ("week".equals(type)) {
            chart.setDates(Arrays.asList("01-08", "01-09", "01-10", "01-11", "01-12", "01-13", "01-14"));
            chart.setOrders(Arrays.asList(45, 52, 48, 60, 55, 58, 62));
            chart.setRevenue(Arrays.asList(1250, 1580, 1420, 1680, 1550, 1620, 1750));
        } else if ("month".equals(type)) {
            chart.setDates(Arrays.asList("12-15", "12-20", "12-25", "12-30", "01-04", "01-09", "01-14"));
            chart.setOrders(Arrays.asList(38, 45, 52, 48, 60, 55, 58));
            chart.setRevenue(Arrays.asList(980, 1250, 1580, 1420, 1680, 1550, 1620));
        }
        return chart;
    }

    /**
     * 获取代金券统计图表数据
     */
    @Override
    public CouponStatsChartVO getCouponStatsChart() {
        // 方式1：真实查询
        // return couponStatsChartMapper.selectCouponStatsChart();
        
        // 方式2：模拟数据（匹配前端mockData）
        CouponStatsChartVO chart = new CouponStatsChartVO();
        chart.setUsed(156);
        chart.setUnused(45);
        chart.setExpired(33);
        return chart;
    }

    /**
     * 获取最新店铺列表
     */
    @Override
    public List<RecentShopVO> getRecentShops(Integer limit) {
        List<ShopDTO> shopDTOList = remoteShopService.getRecentShops(limit).getData();
        List<RecentShopVO> recentShops = new ArrayList<>();
        for (ShopDTO shopDTO : shopDTOList) {
            RecentShopVO recentShop = new RecentShopVO();
            recentShop.setId(shopDTO.getId());
            recentShop.setCategory(shopDTO.getTypeId());
            recentShop.setName(shopDTO.getName());
            recentShop.setAddress(shopDTO.getAddress());
            recentShop.setCreateTime(shopDTO.getCreateTime());
            recentShop.setStatus(1);
            recentShops.add(recentShop);
        }
        return recentShops;
    }

    /**
     * 获取最新订单列表
     */
    @Override
    public List<RecentOrderVO> getRecentOrders(Integer limit) {
        return null;
//        List<VoucherOrderDTO> orderDTOList = remoteOrderService.getRecentOrders(limit).getData();
//        List<RecentOrderVO> recentOrders = new ArrayList<>();
//        for (VoucherOrderDTO orderDTO : orderDTOList) {
//            RecentOrderVO order = new RecentOrderVO();
//            order.setId(orderDTO.getId());
//            order.setOrderNo(orderDTO.getId());
//            order.setShopName(orderDTO.getShopId());
//            order.setAmount(orderDTO.getVoucherId());
//            order.setStatus(orderDTO.getStatus());
//            order.setCreateTime(orderDTO.getCreateTime());
//            recentOrders.add(order);
//        }
//
//        RecentOrderVO order3 = new RecentOrderVO();
//        order3.setId(3L);
//        order3.setOrderNo("DD202401150003");
//        order3.setShopName("西贝莜面村");
//        order3.setAmount(189);
//        order3.setStatus(1);
//        order3.setCreateTime("2024-01-15 12:15:35");
//
//        return ;
    }

    /**
     * 获取最新博客列表
     */
    @Override
    public List<RecentBlogVO> getRecentBlogs(Integer limit) {
        // 方式1：真实查询
        // return recentBlogMapper.selectRecentBlogs(limit);
        
        // 方式2：模拟数据（匹配前端mockData）
        RecentBlogVO blog1 = new RecentBlogVO();
        blog1.setId(1L);
        blog1.setTitle("2024年最值得尝试的10家餐厅，吃货必看！");
        blog1.setSummary("本文为您推荐今年最受欢迎的餐厅，从环境、口味、服务等多个维度进行评价...");
        blog1.setStatus(1);
        blog1.setCreateTime("2024-01-15 10:20:15");

        RecentBlogVO blog2 = new RecentBlogVO();
        blog2.setId(2L);
        blog2.setTitle("冬日暖心美食推荐：火锅的正确打开方式");
        blog2.setSummary("寒冷的冬天最适合吃火锅了，但你知道如何选择最适合自己的火锅店吗？...");
        blog2.setStatus(1);
        blog2.setCreateTime("2024-01-14 15:40:22");

        RecentBlogVO blog3 = new RecentBlogVO();
        blog3.setId(3L);
        blog3.setTitle("探店报告：新开业的日料店究竟如何？");
        blog3.setSummary("实地探访最近新开的一家日料店，从食材新鲜度、服务态度、环境氛围等方面详细评测...");
        blog3.setStatus(0);
        blog3.setCreateTime("2024-01-14 11:25:18");

        return Arrays.asList(blog1, blog2, blog3);
    }
}