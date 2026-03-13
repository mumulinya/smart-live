package com.smartLive.order.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.order.domain.Order;
import com.smartLive.order.domain.VO.ProductSoldVO;
import org.apache.ibatis.annotations.Select;

/**
 * 订单表Mapper接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface OrderMapper extends BaseMapper<Order>
{
    /**
     * 查询订单表
     * 
     * @param id 订单表主键
     * @return 订单表
     */
    public Order selectOrderById(Long id);

    /**
     * 查询订单表列表
     * 
     * @param order 订单表
     * @return 订单表集合
     */
    public List<Order> selectOrderList(Order order);

    /**
     * 新增订单表
     * 
     * @param order 订单表
     * @return 结果
     */
    public int insertOrder(Order order);

    /**
     * 修改订单表
     * 
     * @param order 订单表
     * @return 结果
     */
    public int updateOrder(Order order);

    /**
     * 删除订单表
     * 
     * @param id 订单表主键
     * @return 结果
     */
    public int deleteOrderById(Long id);

    /**
     * 批量删除订单表
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteOrderByIds(Long[] ids);
    /**
     * 查询商品销售量
     * @return
     */
    @Select("""
    SELECT source_id, COUNT(*) as sold_count
    FROM `order`
    WHERE status != 4
    GROUP BY source_id
    """)
    List<ProductSoldVO> countProductSold();

    /**
     * 查询指定商品的累计订单销量
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM `order` WHERE source_id = #{sourceId} AND status != 4")
    Integer sumSoldBySourceId(Long sourceId);

    /**
     * 查询指定店铺的累计订单销量
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM `order` WHERE verify_shop_id = #{shopId} AND status != 4")
    Integer sumSoldByShopId(Long shopId);
}
