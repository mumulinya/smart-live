package com.smartLive.marketing.mapper;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.marketing.domain.Voucher;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 优惠券Mapper接口
 * 
 * @author 木木林
 * @date 2025-09-21
 */
public interface VoucherMapper extends BaseMapper<Voucher>
{
    /**
     * 查询优惠券
     * 
     * @param id 优惠券主键
     * @return 优惠券
     */
     Voucher selectVoucherById(Long id);

    /**
     * 查询优惠券列表
     * 
     * @param voucher 优惠券
     * @return 优惠券集合
     */
     List<Voucher> selectVoucherList(Voucher voucher);

    /**
     * 新增优惠券
     * 
     * @param voucher 优惠券
     * @return 结果
     */
     int insertVoucher(Voucher voucher);

    /**
     * 修改优惠券
     * 
     * @param voucher 优惠券
     * @return 结果
     */
     int updateVoucher(Voucher voucher);

    /**
     * 删除优惠券
     * 
     * @param id 优惠券主键
     * @return 结果
     */
     int deleteVoucherById(Long id);

    /**
     * 批量删除优惠券
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
     int deleteVoucherByIds(Long[] ids);

    /**
     * 查询店铺下的优惠券
     * @param shopId
     * @return
     */
    List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);
    /**
     * 批量更新优惠券的评价数
     * @param updateMap
     */
    @Update("<script>" +
            "UPDATE voucher " +
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
}
