package com.smartLive.shop.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.shop.domain.Shop;

/**
 * 店铺Service接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface IShopService extends IService<Shop> {
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
     * 批量删除店铺
     *
     * @param ids 需要删除的店铺主键集合
     * @return 结果
     */
    public int deleteShopByIds(String[] ids);

    /**
     * 删除店铺信息
     *
     * @param id 店铺主键
     * @return 结果
     */
    public int deleteShopById(String id);


    /**
     * 根据id查询商铺信息
     *
     * @param id 商铺id
     * @return 商铺详情数据
     */
    Result queryById(Long id);

    /**
     * 根据类型分页查询商铺信息
     *
     * @param typeId  商铺类型
     * @param current 页码
     * @return 商铺列表
     */
    Result queryShopByType(Integer typeId, Integer current, String sortBy,Double x, Double y);

    /**
     * 根据商铺信息搜索商铺列表
     *
     * @param shopQuery 搜索条件
     * @return 搜索结果
     */
    List<Shop> searchShopsByShopQuery(Shop shopQuery);

    /**
     * 根据商铺信息查询商铺
     *
     * @param shopVO 商铺信息
     * @return 商铺
     */
    Shop selectShopByShop(Shop shopVO);

    /**
     * 根据商铺名称查询商铺信息
     *
     * @param shopName 商铺名称
     * @return 商铺详情
     */
    R<Shop> getShopByShopName(String shopName);

    /**
     * 修改商铺评论数量
     *
     * @param shopId 商铺id
     * @return 修改结果
     */
    R<Boolean> updateCommentById(Long shopId);

    /**
     * 根据条件查询商铺信息
     *
     * @param shop 搜索条件
     * @return 搜索结果
     */
    List<Shop> getShopByCondition(Shop shop);

    /**
     * 根据商铺id查询商铺信息
     *
     * @param shopId 商铺id
     * @return 商铺信息
     */
    R<Shop> getShopById(Long shopId);

    /**
     * 根据商铺id列表查询商铺信息列表
     *
     * @param ids 商铺id列表
     * @return 商铺列表
     */
    List<Shop> getShopList(List<Long> ids);

    /**
     * 刷新商铺缓存
     *
     * @return 刷新结果
     */
    String flushCache();
    /**
     * 全部发布店铺
     *
     * @return 全部发布结果
     */
    String allPublish();

    /**
     * 发布店铺
     *
     * @param
     * @return 发布结果
     */
    String publish( String[] ids);
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
    List<Shop> getRecentShops(Integer limit);
}


