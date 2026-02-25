package com.smartLive.shop.api;

import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.shop.api.DTO.ShopDTO;
import com.smartLive.shop.api.DTO.ShopTypeDTO;
import com.smartLive.shop.api.factory.RemoteShopFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@FeignClient(contextId = "remoteShopService", value = ServiceNameConstants.SHOP_SERVICE, fallbackFactory = RemoteShopFallbackFactory.class)
public interface RemoteShopService {
    /**
     * 根据商家名称查询商家信息
     */
    @GetMapping("/inner/shop/{shopName}")
    ShopDTO getShopByShopName(@PathVariable("shopName") String shopName);

    /**
     * 根据条件查询商家信息
     */
    @PostMapping("/inner/shop/getShopList")
    List<ShopDTO> queryShopList(@RequestBody ShopDTO shopDTo);

    /**
     * 查询商铺类型列表
     */
    @GetMapping("/inner/shop/shop-type/getShopListByType")
    List<ShopTypeDTO> getShopTypeList();


    /**
     * 根据商家Id查询商家信息
     */
    @GetMapping("/inner/shop/getShopById/{shopId}")
    ShopDTO getShopById(@PathVariable("shopId") Long shopId);

    @GetMapping("/inner/shop/shopListByIds")
    List<ShopDTO> getShopList(@RequestParam("shopIdList") List<Long> shopIdList);

    /**
     * 获取商家总数
     */
    @GetMapping("/inner/shop/getShopTotal")
    Integer getShopTotal();

    /**
     * 获取最近创建商家
     */
    @GetMapping("/inner/shop/getRecentShops")
    List<ShopDTO> getRecentShops(@RequestParam("limit") Integer limit);

    /**
     * 批量更新商家评价数
     */
    @PostMapping("/inner/shop/updateReviewCountBatch")
    Boolean updateReviewCountBatch(@RequestBody Map<Long, Integer> updateMap);

    /**
     * 批量更新商家收藏数
     */
    @PostMapping("/inner/shop/updateStarCountBatch")
    Boolean updateStarCountBatch(@RequestBody Map<Long, Integer> updateMap);

    /**
     * 获取商家收藏数
     */
    @GetMapping("/inner/shop/getStarCount/{sourceId}")
    Integer getStarCount(@PathVariable("sourceId") Long sourceId);

    /**
     * 更新店铺状态
     */
    @PostMapping("/inner/shop/updateShopStatus")
    Boolean updateShopStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status);

    /**
     * 获取全部店铺ID列表
     */
    @GetMapping("/inner/shop/getAllShopIds")
    List<Long> getAllShopIds();
}
