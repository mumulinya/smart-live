package com.smartLive.shop.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.shop.domain.Shop;
import com.smartLive.shop.domain.VO.ShopAnalysisVO;
import com.smartLive.shop.domain.VO.ShopSuggestVO;
import com.smartLive.shop.domain.VO.ShopVO;

/**
 * 店铺业务契约接口
 * 定义了店铺的基础维护、多级缓存管理（含穿透/击穿解决）、热门排行榜计算及地理位置检索逻辑。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
public interface IShopService extends IService<Shop> {
    /**
     * 查询店铺
     *
     * @param id 店铺主键
     * @return 店铺
     */
    Shop selectShopById(String id);

    /**
     * 查询店铺列表
     *
     * @param shop 店铺
     * @return 店铺集合
     */
    List<Shop> selectShopList(Shop shop);

    /**
     * 新增店铺
     *
     * @param shop 店铺
     * @return 结果
     */
    int insertShop(Shop shop);

    /**
     * 修改店铺
     *
     * @param shop 店铺
     * @return 结果
     */
    int updateShop(Shop shop);

    /**
     * 批量删除店铺
     *
     * @param ids 需要删除的店铺主键集合
     * @return 结果
     */
    int deleteShopByIds(String[] ids);

    /**
     * 删除店铺信息
     *
     * @param id 店铺主键
     * @return 结果
     */
    int deleteShopById(String id);


    /**
     * 根据主键查询商铺详情 (整合缓存逻辑)
     * 支持通过逻辑过期及互斥锁策略解决高并发下的缓存击穿问题。
     *
     * @param id 商铺 ID
     * @return 商铺详情 VO (含收藏、关注状态)
     */
    ShopVO queryById(Long id);

    /**
     * 根据商铺名称查询商铺信息
     *
     * @param shopName 商铺名称
     * @return 商铺详情
     */
    ShopVO getShopByShopName(String shopName);

    /**
     * 根据条件查询商铺信息
     *
     * @param shop 搜索条件
     * @return 搜索结果
     */
    List<ShopVO> getShopByCondition(Shop shop);

    /**
     * 根据商铺id列表查询商铺信息列表
     *
     * @param ids 商铺id列表
     * @return 商铺列表
     */
    List<ShopVO> getShopList(List<Long> ids);

    /**
     * 手动重置并刷新商铺全量缓存
     * 包含店铺基本信息缓存及地理位置 (GEO) 索引缓存。
     *
     * @return 执行结果描述
     */
    String flushCache();

    /**
     * 全部发布店铺
     *
     * @return 全部发布结果
     */
    String allPublish();

    /**
     * 批量发布店铺至ES和Milvus索引
     *
     * @param ids 店铺ID数组
     * @return 发布结果
     */
    String publish(String[] ids);

    /**
     * 获取商铺总数
     *
     * @return 商铺总数
     */
    Integer getShopTotal();

    /**
     * 获取最近商铺
     *
     * @param limit 获取数量
     * @return 最近商铺
     */
    List<ShopVO> getRecentShops(Integer limit);

    /**
     * 批量更新商铺收藏数
     *
     * @param updateMap 商铺id和收藏数
     * @return 更新结果
     */
    Boolean updateStarCountBatch(Map<Long, Integer> updateMap);

    /**
     * Batch update shop fans count.
     *
     * @param updateMap shopId -> fansCount
     * @return update result
     */
    Boolean updateFansCountBatch(Map<Long, Integer> updateMap);

    /**
     * 批量更新商铺评价数
     *
     * @param updateMap 商铺id和评价数
     * @return 批量更新结果
     */
    Boolean updateReviewCountBatch(Map<Long, Integer> updateMap);

    /**
     * 更新店铺状态
     *
     * @param id     店铺ID
     * @param status 状态
     * @param reason 拒绝原因（通过时为null）
     * @return 结果
     */
    Boolean updateShopStatus(Long id, Integer status, String reason);

    /**
     * 获取热门店铺排行榜
     * 结合 Redis 热度分序列与用户地理位置 (x,y) 计算综合排名。
     *
     * @param current 页码
     * @param size    每页数量
     * @param x       用户当前经度 (可选)
     * @param y       用户当前纬度 (可选)
     * @return 包含热度评分与距离信息的店铺 VO 列表
     */
    List<ShopVO> getHotShopRank(Integer current, Integer size, Double x, Double y);

    /**
     * 批量更新销量
     *
     * @param updateMap 店铺id和销量
     * @return 结果
     */
    Boolean updateSoldBatch(Map<Long, Integer> updateMap);

    /**
     * 同步销量数据从 Redis 到数据库
     */
    void syncSalesData();

    /**
     * 根据用户id查询所属店铺
     * @param userId
     * @param shop
     * @return
     */
    List<Shop> selectShopListByUserId(Long userId, Shop shop);
    ShopAnalysisVO getShopAnalysis(Long shopId, String timeRange);

    ShopSuggestVO getShopSuggest(Long shopId);
}