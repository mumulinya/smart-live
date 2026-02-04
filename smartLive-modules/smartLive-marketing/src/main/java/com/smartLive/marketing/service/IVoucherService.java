package com.smartLive.marketing.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.marketing.domain.VO.VoucherVO;
import com.smartLive.marketing.domain.Voucher;

/**
 * 优惠券Service接口
 * 
 * @author 木木林
 * @date 2025-09-21
 */
public interface IVoucherService extends IService<Voucher>
{
    /**
     * 查询优惠券
     * 
     * @param id 优惠券主键
     * @return 优惠券
     */
     VoucherVO selectVoucherById(Long id);
    /**
     * 鏌ヨ浼樻儬鍒?锛堢粰鍐呴儴鎺ュ彛浣跨敤锛?
     *
     * @param id 浼樻儬鍒镐富閿?
     * @return 浼樻儬鍒?
     */
     Voucher selectVoucherEntityById(Long id);

    /**
     * 查询优惠券列表
     * 
     * @param voucher 优惠券
     * @return 优惠券集合
     */
     List<Voucher> selectVoucherEntityList(Voucher voucher);
    /**
     * 鏌ヨ浼樻儬鍒稿垪琛?
     *
     * @param voucher 浼樻儬鍒?
     * @return 浼樻儬鍒搴旂敤VO闆嗗悎
     */
     List<VoucherVO> selectVoucherList(Voucher voucher);

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
     * 批量删除优惠券
     * 
     * @param ids 需要删除的优惠券主键集合
     * @return 结果
     */
     int deleteVoucherByIds(Long[] ids);

    /**
     * 删除优惠券信息
     * 
     * @param id 优惠券主键
     * @return 结果
     */
     int deleteVoucherById(Long id);

    /**
     * 根据店铺查询优惠券列表
     * @param shopId
     * @return
     */
    List<VoucherVO> queryVoucherOfShop(Long shopId);

    /**
     * 添加秒杀券
     * @param voucher
     */
    boolean addSeckillVoucher(Voucher voucher);

    /**
     * 秒杀优惠券
     * @param voucherId
     * @return
     */
    Long seckillVoucher(Long voucherId, Long userId);


    /**
     * 购买优惠券
     * @param voucherId
     * @return
     */
    Long buyVoucher(Long voucherId, Long userId);

    /**
     * 查询店铺的优惠券列表
     * @param
     * @return
     */

    List<Voucher> listVoucher();

    /**
     * 查询店铺的秒杀优惠券列表
     * @param voucher
     * @return
     */

    List<Voucher> listSeckillVoucher(Voucher voucher);

    /**
     * 全部发布
     *
     * @return 全部发布结果
     */
    String allPublish();

    /**
     * 发布
     *
     * @param
     * @return 发布结果
     */
    String publish( String[] ids);
    /**
     * 获取优惠券总数
     *
     * @return 优惠券总数
     */
    Integer getCouponTotal();
    /**
     * 获取优惠券列表
     *
     * @param sourceIdList 优惠券id列表
     * @return 优惠券列表
     */
    List<Voucher> getVoucherListByIds(List<Long> sourceIdList);
    /**
     * 获取优惠券
     *
     * @param id 优惠券id
     * @return 优惠券
     */
    VoucherVO getVoucherById(Long id);
    /**
     * 添加库存
     *
     * @param id 优惠券id
     * @return 添加结果
     */
    int addStock(Long id);
    /**
     * 修改优惠券状态
     *
     * @param voucher 优惠券
     * @return 修改结果
     */
    Boolean changeStatus(Voucher voucher);
    /**
     * 优惠券价格下降
     *
     * @param id 优惠券id
     * @return 优惠券价格下降结果
     */
    int priceReduced(Long id);
    /**
     * 批量更新评价数
     *
     * @param updateMap 批量更新评价数
     * @return 批量更新评价数结果
     */
    Boolean updateReviewCountBatch(Map<Long, Integer> updateMap);
    /**
     * 批量更新代金券收藏数
     *
     * @param updateMap 商铺id和收藏数
     * @return 更新结果
     */
    Boolean updateStarCountBatch(Map<Long, Integer> updateMap);
    /**
     * 获取代金券收藏数
     *
     * @param sourceId 优惠券id
     * @return 收藏数
     */
    Integer getVoucherStarCount(Long sourceId);
}
