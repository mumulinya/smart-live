package com.smartLive.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.order.domain.Order;
import com.smartLive.order.domain.VO.ProductSalesVO;
import com.smartLive.order.domain.VO.ProductSoldVO;
import com.smartLive.order.domain.VO.ShopOrderAnalysisVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单数据访问层。
 */
public interface OrderMapper extends BaseMapper<Order> {
    /**
     * 根据ID查询订单详情。
     *
     * @param id 订单ID
     * @return 订单信息
     */
    Order selectOrderById(Long id);

    /**
     * 查询订单列表。
     *
     * @param order 查询条件
     * @return 订单列表
     */
    List<Order> selectOrderList(Order order);

    /**
     * 新增订单。
     *
     * @param order 订单信息
     * @return 影响行数
     */
    int insertOrder(Order order);

    /**
     * 修改订单。
     *
     * @param order 订单信息
     * @return 影响行数
     */
    int updateOrder(Order order);

    /**
     * 删除订单信息。
     *
     * @param id 订单ID
     * @return 影响行数
     */
    int deleteOrderById(Long id);

    /**
     * 批量删除订单。
     *
     * @param ids 订单ID数组
     * @return 影响行数
     */
    int deleteOrderByIds(Long[] ids);

    /**
     * 统计商品销量。
     *
     * @return 商品销量列表
     */
    @Select("""
    SELECT source_id, COUNT(*) as sold_count
    FROM `order`
    WHERE status != 4
    GROUP BY source_id
    """)
    List<ProductSoldVO> countProductSold();

    /**
     * 根据来源ID统计销量数量。
     *
     * @param sourceId 来源ID
     * @return 销量数量
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM `order` WHERE source_id = #{sourceId} AND status != 4")
    Integer sumSoldBySourceId(Long sourceId);

    /**
     * 根据店铺ID统计销量数量。
     *
     * @param shopId 店铺ID
     * @return 销量数量
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM `order` WHERE verify_shop_id = #{shopId} AND status != 4")
    Integer sumSoldByShopId(Long shopId);

    /**
     * 统计店铺本周核销订单数。
     *
     * @param shopId 店铺ID
     * @return 订单数量
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
     * 查询店铺订单分析数据。
     *
     * @param shopId 店铺ID
     * @param statusList 订单状态集合
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 店铺订单分析
     */
    ShopOrderAnalysisVO selectShopOrderAnalysis(@Param("shopId") Long shopId,
                                                @Param("statusList") List<Integer> statusList,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);

    /**
     * 查询店铺热销商品。
     *
     * @param shopId 店铺ID
     * @param statusList 订单状态集合
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 限制数量
     * @return 商品销量列表
     */
    List<ProductSalesVO> selectShopHotProducts(@Param("shopId") Long shopId,
                                               @Param("statusList") List<Integer> statusList,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime,
                                               @Param("limit") Integer limit);

    /**
     * 查询店铺滞销商品。
     *
     * @param shopId 店铺ID
     * @param statusList 订单状态集合
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 限制数量
     * @return 商品销量列表
     */
    List<ProductSalesVO> selectShopSlowProducts(@Param("shopId") Long shopId,
                                                @Param("statusList") List<Integer> statusList,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime,
                                                @Param("limit") Integer limit);

    /**
     * 查询店铺复购率。
     *
     * @param shopId 店铺ID
     * @param statusList 订单状态集合
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 复购率
     */
    BigDecimal selectShopRepurchaseRate(@Param("shopId") Long shopId,
                                        @Param("statusList") List<Integer> statusList,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);
}
