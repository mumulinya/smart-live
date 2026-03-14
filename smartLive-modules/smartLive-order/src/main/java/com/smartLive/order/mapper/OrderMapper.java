package com.smartLive.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.order.domain.Order;
import com.smartLive.order.domain.VO.ProductSalesVO;
import com.smartLive.order.domain.VO.ProductSoldVO;
import com.smartLive.order.domain.VO.ShopOrderAnalysisVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderMapper extends BaseMapper<Order> {
    Order selectOrderById(Long id);

    List<Order> selectOrderList(Order order);

    int insertOrder(Order order);

    int updateOrder(Order order);

    int deleteOrderById(Long id);

    int deleteOrderByIds(Long[] ids);

    @Select("""
    SELECT source_id, COUNT(*) as sold_count
    FROM `order`
    WHERE status != 4
    GROUP BY source_id
    """)
    List<ProductSoldVO> countProductSold();

    @Select("SELECT COALESCE(SUM(amount), 0) FROM `order` WHERE source_id = #{sourceId} AND status != 4")
    Integer sumSoldBySourceId(Long sourceId);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM `order` WHERE verify_shop_id = #{shopId} AND status != 4")
    Integer sumSoldByShopId(Long shopId);

    @Select("""
    SELECT COUNT(*)
    FROM `order`
    WHERE verify_shop_id = #{shopId}
      AND status = 3
      AND use_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)
    """)
    Integer countWeekOrders(Long shopId);

    ShopOrderAnalysisVO selectShopOrderAnalysis(@Param("shopId") Long shopId,
                                                @Param("statusList") List<Integer> statusList,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);

    List<ProductSalesVO> selectShopHotProducts(@Param("shopId") Long shopId,
                                               @Param("statusList") List<Integer> statusList,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime,
                                               @Param("limit") Integer limit);

    List<ProductSalesVO> selectShopSlowProducts(@Param("shopId") Long shopId,
                                                @Param("statusList") List<Integer> statusList,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime,
                                                @Param("limit") Integer limit);
}