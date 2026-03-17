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
    /**
     * 查询订单详情
     */
    Order selectOrderById(Long id);
    /**
     * 查询订单列表
     */

    List<Order> selectOrderList(Order order);
    /**
     * 新增订单
     */

    int insertOrder(Order order);
    /**
     * 修改订单
     */

    int updateOrder(Order order);
    /**
     * 删除订单信息
     */

    int deleteOrderById(Long id);
    /**
     * 批量删除订单
     */

    int deleteOrderByIds(Long[] ids);
    /**
     * 统计商品销量
     */

    @Select("""
    SELECT source_id, COUNT(*) as sold_count
    FROM `order`
    WHERE status != 4
    GROUP BY source_id
    """)
    List<ProductSoldVO> countProductSold();
    /**
     * 根据来源ID统计销量金额
     */

    @Select("SELECT COALESCE(SUM(amount), 0) FROM `order` WHERE source_id = #{sourceId} AND status != 4")
    Integer sumSoldBySourceId(Long sourceId);
    /**
     * 根据店铺ID统计销量金额
     */

    @Select("SELECT COALESCE(SUM(amount), 0) FROM `order` WHERE verify_shop_id = #{shopId} AND status != 4")
    Integer sumSoldByShopId(Long shopId);
    /**
     * 统计店铺本周订单数
     */

    @Select("""
    SELECT COUNT(*)
    FROM `order`
    WHERE verify_shop_id = #{shopId}
      AND status = 3
      AND use_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)
    """)
    Integer countWeekOrders(Long shopId);
    /**
     * 查询店铺订单分析数据
     */

    ShopOrderAnalysisVO selectShopOrderAnalysis(@Param("shopId") Long shopId,
    /**
     * 查询店铺热销商品
     */
                                                @Param("statusList") List<Integer> statusList,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);

    List<ProductSalesVO> selectShopHotProducts(@Param("shopId") Long shopId,
    /**
     * 查询店铺滞销商品
     */
                                               @Param("statusList") List<Integer> statusList,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime,
                                               @Param("limit") Integer limit);

    List<ProductSalesVO> selectShopSlowProducts(@Param("shopId") Long shopId,
                                                @Param("statusList") List<Integer> statusList,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime,
                                                @Param("limit") Integer limit);

    java.math.BigDecimal selectShopRepurchaseRate(@Param("shopId") Long shopId,
                                                  @Param("statusList") List<Integer> statusList,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);
}
