package com.smartLive.shop.mapper;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.shop.domain.Shop;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 店铺数据访问接口。
 */
public interface ShopMapper extends BaseMapper<Shop>
{
    /**
     * 根据ID查询店铺实体。
     */
    public Shop selectShopById(Long id);

    /**
     * 查询店铺实体列表。
     */
    public List<Shop> selectShopList(Shop shop);

    /**
     * 新增店铺。
     */
    public int insertShop(Shop shop);

    /**
     * 更新店铺。
     */
    public int updateShop(Shop shop);

    /**
     * 根据ID删除店铺。
     */
    public int deleteShopById(Long id);

    /**
     * 批量删除店铺。
     */
    public int deleteShopByIds(Long[] ids);
    /**
     * 批量更新店铺评价数。
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
     * 批量更新店铺收藏数。
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
     * 批量更新店铺粉丝数。
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

    /**
     * 批量更新店铺销量统计。
     */
    @Update("<script>" +
            "UPDATE shop " +
            "SET sold" +
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
    void updateSoldBatch(@Param("map") Map<Long, Integer> updateMap);

    /**
     * 根据店铺ID集合和条件查询店铺列表。
     */
    List<Shop> selectShopListByIdsAndCondition(@Param("shopIds") List<Long> shopIds, @Param("shop") Shop shop);
}
