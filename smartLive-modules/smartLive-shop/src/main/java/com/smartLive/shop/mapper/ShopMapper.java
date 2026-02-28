package com.smartLive.shop.mapper;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.shop.domain.Shop;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 店铺Mapper接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface ShopMapper extends BaseMapper<Shop>
{
    /**
     * 查询店铺
     * 
     * @param id 店铺主键
     * @return 店铺
     */
    public Shop selectShopById(String id);

    /**
     * 查询店铺列表
     * 
     * @param shop 店铺
     * @return 店铺集合
     */
    public List<Shop> selectShopList(Shop shop);

    /**
     * 新增店铺
     * 
     * @param shop 店铺
     * @return 结果
     */
    public int insertShop(Shop shop);

    /**
     * 修改店铺
     * 
     * @param shop 店铺
     * @return 结果
     */
    public int updateShop(Shop shop);

    /**
     * 删除店铺
     * 
     * @param id 店铺主键
     * @return 结果
     */
    public int deleteShopById(String id);

    /**
     * 批量删除店铺
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteShopByIds(String[] ids);
    /**
     * 批量更新店铺的评价数
     * @param updateMap
     */
    @Update("<script>" +
            "UPDATE shop " +
            "SET reviews" +
            " = CASE id " +
            "  <foreach collection='map.entrySet()' index='key' item='val'> " +
            "    WHEN #{key} THEN #{val} " +
            "  </foreach> " +
            "END " +
            "WHERE id IN " +
            "  <foreach collection='map.keySet()' item='key' open='(' separator=',' close=')'> " +
            "    #{key} " +
            "  </foreach>" +
            "</script>")
    void updateReviewCountBatch(@Param("map") Map<Long, Integer> updateMap);
    /**
     * 批量更新店铺的收藏数
     * @param
     */
    @Update("<script>" +
            "UPDATE shop " +
            "SET stared" +
            " = CASE id " +
            "  <foreach collection='map.entrySet()' index='key' item='val'> " +
            "    WHEN #{key} THEN #{val} " +
            "  </foreach> " +
            "END " +
            "WHERE id IN " +
            "  <foreach collection='map.keySet()' item='key' open='(' separator=',' close=')'> " +
            "    #{key} " +
            "  </foreach>" +
            "</script>")
    void updateStarCountBatch(@Param("map")Map<Long, Integer> batchMap);

    /**
     * Batch update shop fans count.
     *
     * @param batchMap shopId -> fansCount
     */
    @Update("<script>" +
            "UPDATE shop " +
            "SET fans" +
            " = CASE id " +
            "  <foreach collection='map.entrySet()' index='key' item='val'> " +
            "    WHEN #{key} THEN #{val} " +
            "  </foreach> " +
            "END " +
            "WHERE id IN " +
            "  <foreach collection='map.keySet()' item='key' open='(' separator=',' close=')'> " +
            "    #{key} " +
            "  </foreach>" +
            "</script>")
    void updateFansCountBatch(@Param("map") Map<Long, Integer> batchMap);
}
