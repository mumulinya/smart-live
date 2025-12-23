package com.smartLive.shop.api;
import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.common.core.domain.R;
import com.smartLive.shop.api.domain.ShopDTO;
import com.smartLive.shop.api.domain.ShopTypeDTO;
import com.smartLive.shop.api.factory.RemoteShopFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(contextId = "remoteShopService", value = ServiceNameConstants.SHOP_SERVICE, fallbackFactory = RemoteShopFallbackFactory.class)
public interface RemoteShopService {
    /**
     * 根据商家名称查询商家信息
     */
    @GetMapping("/shop/{shopName}")
    public R<ShopDTO> getShopByShopName(@PathVariable("shopName") String shopName);

    /**
     * 更新商家评论数
     */
    @PostMapping("/shop/updateCommentById/{id}")
    public R<Boolean> updateCommentById(@PathVariable("id") Long shopId);

    /**
     * 根据条件查询商家信息
     */
    @PostMapping("/shop/getShopList")
    public R<List<ShopDTO>> queryShopList(@RequestBody ShopDTO shopDTo);

    /**
     * 查询商铺类型列表
     */
    @GetMapping("/shop-type/getShopListByType")
    public R<List<ShopTypeDTO>> getShopTypeList();


    /**
     * 根据商家Id查询商家信息
     */
    @GetMapping("/shop/getShopById/{shopId}")
    public R<ShopDTO> getShopById(@PathVariable("shopId") Long shopId);

    @GetMapping("/shop/shopListByIds")
    R<List<ShopDTO>> getShopList(@RequestParam("shopIdList") List<Long> shopIdList);

    /**
     * 获取商家总数
     */
    @GetMapping("/shop/getShopTotal")
    public R<Integer> getShopTotal();

    /**
     * 获取最近创建商家
     */
    @GetMapping("/shop/getRecentShops")
    public R<List<ShopDTO>> getRecentShops(@RequestParam("limit") Integer limit);

    /**
     * 批量更新商家评论数
     */
    @PostMapping("/shop/updateCommentCountBatch")
    public R<Boolean> updateCommentCountBatch(@RequestBody Map<Long, Integer> updateMap);
}
