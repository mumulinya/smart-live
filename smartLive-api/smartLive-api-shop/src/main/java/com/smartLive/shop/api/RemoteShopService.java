package com.smartLive.shop.api;

import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.shop.api.DTO.ShopAnalysisDTO;
import com.smartLive.shop.api.DTO.ShopDTO;
import com.smartLive.shop.api.DTO.ShopSuggestDTO;
import com.smartLive.shop.api.DTO.ShopTypeDTO;
import com.smartLive.shop.api.factory.RemoteShopFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(contextId = "remoteShopService", value = ServiceNameConstants.SHOP_SERVICE, fallbackFactory = RemoteShopFallbackFactory.class)
public interface RemoteShopService {

    @GetMapping("/inner/shop/{shopName}")
    ShopDTO getShopByShopName(@PathVariable("shopName") String shopName);

    @PostMapping("/inner/shop/getShopList")
    List<ShopDTO> queryShopList(@RequestBody ShopDTO shopDTo);

    @GetMapping("/inner/shop/shop-type/getShopListByType")
    List<ShopTypeDTO> getShopTypeList();

    @GetMapping("/inner/shop/getShopById/{shopId}")
    ShopDTO getShopById(@PathVariable("shopId") Long shopId);

    @GetMapping("/inner/shop/shopListByIds")
    List<ShopDTO> getShopList(@RequestParam("shopIdList") List<Long> shopIdList);

    @GetMapping("/inner/shop/getShopTotal")
    Integer getShopTotal();

    @GetMapping("/inner/shop/getRecentShops")
    List<ShopDTO> getRecentShops(@RequestParam("limit") Integer limit);

    @PostMapping("/inner/shop/updateReviewCountBatch")
    Boolean updateReviewCountBatch(@RequestBody Map<Long, Integer> updateMap);

    @PostMapping("/inner/shop/updateStarCountBatch")
    Boolean updateStarCountBatch(@RequestBody Map<Long, Integer> updateMap);

    @PostMapping("/inner/shop/updateFansCountBatch")
    Boolean updateFansCountBatch(@RequestBody Map<Long, Integer> updateMap);

    @GetMapping("/inner/shop/getStarCount/{sourceId}")
    Integer getStarCount(@PathVariable("sourceId") Long sourceId);

    @PostMapping("/inner/shop/updateShopStatus")
    Boolean updateShopStatus(@RequestParam("id") Long id,
                             @RequestParam("status") Integer status,
                             @RequestParam(value = "reason", required = false) String reason);

    @GetMapping("/inner/shop/getAllShopIds")
    List<Long> getAllShopIds();

    @PostMapping("/inner/shop/updateSoldBatch")
    Boolean updateSoldBatch(@RequestBody Map<Long, Integer> updateMap);

    @GetMapping("/inner/shop/getSold/{id}")
    Integer getSold(@PathVariable("id") Long id);

    @GetMapping("/inner/shop/analysis")
    ShopAnalysisDTO getShopAnalysis(@RequestParam("shopId") Long shopId,
                                    @RequestParam(value = "timeRange", defaultValue = "week") String timeRange);

    @GetMapping("/inner/shop/suggest")
    ShopSuggestDTO getShopSuggest(@RequestParam("shopId") Long shopId,
                                  @RequestParam(value = "timeRange", defaultValue = "week") String timeRange);
}