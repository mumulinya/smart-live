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
     * 根据ID查询店铺实体。
     */
    Shop selectShopById(Long id);

    /**
     * 查询店铺实体列表。
     */
    List<Shop> selectShopList(Shop shop);

    /**
     * 新增店铺。
     */
    int insertShop(Shop shop);

    /**
     * 更新店铺。
     */
    int updateShop(Shop shop);

    /**
     * 批量删除店铺。
     */
    int deleteShopByIds(Long[] ids);

    /**
     * 根据ID删除店铺。
     */
    int deleteShopById(Long id);

    /**
     * 查询店铺详情。
     */
    ShopVO queryById(Long id);

    /**
     * 根据店铺名称查询店铺。
     */
    ShopVO getShopByShopName(String shopName);

    /**
     * 根据条件查询店铺。
     */
    List<ShopVO> getShopByCondition(Shop shop);

    /**
     * 批量查询店铺列表。
     */
    List<ShopVO> getShopList(List<Long> ids);

    /**
     * 刷新店铺缓存。
     */
    String flushCache();

    /**
     * 发布店铺数据到搜索服务。
     */
    String allPublish();

    /**
     * 按ID发布店铺数据到搜索服务。
     */
    String publish(String[] ids);

    /**
     * 获取店铺总数。
     */
    Integer getShopTotal();

    /**
     * 查询最近新增的店铺。
     */
    List<ShopVO> getRecentShops(Integer limit);

    /**
     * 批量更新店铺收藏数。
     */
    Boolean updateStarCountBatch(Map<Long, Integer> updateMap);

    /**
     * 批量更新店铺粉丝数。
     */
    Boolean updateFansCountBatch(Map<Long, Integer> updateMap);

    /**
     * 批量更新店铺评价数。
     */
    Boolean updateReviewCountBatch(Map<Long, Integer> updateMap);

    /**
     * 更新店铺状态。
     */
    Boolean updateShopStatus(Long id, Integer status, String reason);

    /**
     * 查询热门店铺排行榜。
     */
    List<ShopVO> getHotShopRank(Integer current, Integer size, Double x, Double y);

    /**
     * 批量更新店铺销量。
     */
    Boolean updateSoldBatch(Map<Long, Integer> updateMap);

    /**
     * 同步店铺销量数据。
     */
    void syncSalesData();

    /**
     * 根据用户ID查询店铺列表。
     */
    List<Shop> selectShopListByUserId(Long userId, Shop shop);

    /**
     * 按关键词搜索店铺。
     */
    List<ShopVO> searchShops(String keyword);
}
