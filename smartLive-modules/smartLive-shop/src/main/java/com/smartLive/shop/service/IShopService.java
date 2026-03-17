package com.smartLive.shop.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.shop.domain.Shop;
import com.smartLive.shop.domain.VO.ShopVO;

/**
 * 店铺服务接口。
 */
public interface IShopService extends IService<Shop> {
    /**
     * 根据店铺ID查询店铺实体。
     *
     * @param id 店铺ID
     * @return 店铺实体
     */
    Shop selectShopById(Long id);

    /**
     * 查询店铺实体列表。
     *
     * @param shop 查询条件
     * @return 店铺实体列表
     */
    List<Shop> selectShopList(Shop shop);

    /**
     * 新增店铺。
     *
     * @param shop 店铺信息
     * @return 影响行数
     */
    int insertShop(Shop shop);

    /**
     * 修改店铺。
     *
     * @param shop 店铺信息
     * @return 影响行数
     */
    int updateShop(Shop shop);

    /**
     * 批量删除店铺。
     *
     * @param ids 店铺ID数组
     * @return 影响行数
     */
    int deleteShopByIds(Long[] ids);

    /**
     * 根据店铺ID删除店铺。
     *
     * @param id 店铺ID
     * @return 影响行数
     */
    int deleteShopById(Long id);

    /**
     * 查询店铺详情。
     *
     * @param id 店铺ID
     * @return 店铺详情
     */
    ShopVO queryById(Long id);

    /**
     * 根据店铺名称查询店铺。
     *
     * @param shopName 店铺名称
     * @return 店铺详情
     */
    ShopVO getShopByShopName(String shopName);

    /**
     * 根据条件查询店铺列表。
     *
     * @param shop 查询条件
     * @return 店铺列表
     */
    List<ShopVO> getShopByCondition(Shop shop);

    /**
     * 根据店铺ID列表批量查询店铺。
     *
     * @param ids 店铺ID列表
     * @return 店铺列表
     */
    List<ShopVO> getShopList(List<Long> ids);

    /**
     * 刷新店铺缓存。
     *
     * @return 执行结果
     */
    String flushCache();

    /**
     * 全量发布店铺数据到搜索索引。
     *
     * @return 执行结果
     */
    String allPublish();

    /**
     * 发布指定店铺数据到搜索索引。
     *
     * @param ids 店铺ID数组
     * @return 执行结果
     */
    String publish(String[] ids);

    /**
     * 获取店铺总数。
     *
     * @return 店铺总数
     */
    Integer getShopTotal();

    /**
     * 查询最近新增的店铺。
     *
     * @param limit 数量限制
     * @return 店铺列表
     */
    List<ShopVO> getRecentShops(Integer limit);

    /**
     * 批量更新店铺收藏数。
     *
     * @param updateMap 店铺ID与收藏数映射
     * @return 更新结果
     */
    Boolean updateStarCountBatch(Map<Long, Integer> updateMap);

    /**
     * 批量更新店铺粉丝数。
     *
     * @param updateMap 店铺ID与粉丝数映射
     * @return 更新结果
     */
    Boolean updateFansCountBatch(Map<Long, Integer> updateMap);

    /**
     * 批量更新店铺评价数。
     *
     * @param updateMap 店铺ID与评价数映射
     * @return 更新结果
     */
    Boolean updateReviewCountBatch(Map<Long, Integer> updateMap);

    /**
     * 更新店铺状态。
     *
     * @param id 店铺ID
     * @param status 状态值
     * @param reason 原因说明
     * @return 更新结果
     */
    Boolean updateShopStatus(Long id, Integer status, String reason);

    /**
     * 查询热门店铺排行。
     *
     * @param current 当前页
     * @param size 每页数量
     * @param x 经度
     * @param y 纬度
     * @return 店铺排行列表
     */
    List<ShopVO> getHotShopRank(Integer current, Integer size, Double x, Double y);

    /**
     * 批量更新店铺销量。
     *
     * @param updateMap 店铺ID与销量映射
     * @return 更新结果
     */
    Boolean updateSoldBatch(Map<Long, Integer> updateMap);

    /**
     * 同步店铺销量数据。
     */
    void syncSalesData();

    /**
     * 根据用户ID查询店铺列表。
     *
     * @param userId 用户ID
     * @param shop 查询条件
     * @return 店铺列表
     */
    List<Shop> selectShopListByUserId(Long userId, Shop shop);

    /**
     * 获取店铺经营分析数据。
     *
     * @param shopId 店铺ID
     * @param timeRange 时间范围
     * @return 经营分析数据
     */
    /**
     * 获取店铺经营建议数据。
     *
     * @param shopId 店铺ID
     * @param timeRange 时间范围
     * @return 经营建议数据
     */

    /**
     * 按店铺名称模糊搜索店铺列表。
     *
     * @param keyword 搜索关键词
     * @return 店铺列表
     */
    List<ShopVO> searchShops(String keyword);
}
