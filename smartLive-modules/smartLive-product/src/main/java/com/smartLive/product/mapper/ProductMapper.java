package com.smartLive.product.mapper;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.product.domain.Product;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品Mapper接口
 * 
 * @author 桃桃
 * @date 2026-02-18
 */
public interface ProductMapper extends BaseMapper<Product>
{
    /**
     * 查询商品
     * 
     * @param id 商品主键
     * @return 商品
     */
     Product selectProductById(Long id);

    /**
     * 查询商品列表
     * 
     * @param product 商品
     * @return 商品集合
     */
     List<Product> selectProductList(Product product);

    /**
     * 新增商品
     * 
     * @param product 商品
     * @return 结果
     */
     int insertProduct(Product product);

    /**
     * 修改商品
     * 
     * @param product 商品
     * @return 结果
     */
     int updateProduct(Product product);

    /**
     * 删除商品
     * 
     * @param id 商品主键
     * @return 结果
     */
     int deleteProductById(Long id);

    /**
     * 批量删除商品
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
     int deleteProductByIds(Long[] ids);


    /**
     * 批量更新商品的评价数
     * @param updateMap
     */
    @Update("<script>" +
            "UPDATE product " +
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
     * @param batchMap
     */
    @Update("<script>" +
            "UPDATE product " +
            "SET stars" +
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
    void updateStarCountBatch(@Param("map") Map<Long, Integer> batchMap);

    /**
     * Batch update product fans count.
     */
    @Update("<script>" +
            "UPDATE product " +
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
