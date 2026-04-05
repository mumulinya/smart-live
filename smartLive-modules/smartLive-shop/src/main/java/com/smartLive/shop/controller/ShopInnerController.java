package com.smartLive.shop.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.enums.interaction.StarTypeEnum;
import com.smartLive.common.core.enums.product.SalesTypeEnum;
import com.smartLive.common.core.utils.poi.ExcelUtil;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.common.core.web.page.TableDataInfo;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.common.security.annotation.RequiresPermissions;
import com.smartLive.shop.domain.Shop;
import com.smartLive.shop.domain.VO.ShopVO;
import com.smartLive.shop.service.IShopService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 店铺内部接口控制器。
 */
@RestController
@RequestMapping("/inner/shop")
public class ShopInnerController extends BaseController {
    @Autowired
    private IShopService shopService;
    @Autowired
    private RedisService redisService;

    /**
     * 根据店铺名称查询店铺。
     */
    @GetMapping("/{shopName}")
    public ShopVO getShopByShopName(@PathVariable("shopName") String shopName){
        return shopService.getShopByShopName(shopName);
    }

    /**
     * 根据条件查询店铺。
     */
    @PostMapping("/getShopList")
    public List<ShopVO> getShopByCondition(@RequestBody Shop shop){
        return shopService.getShopByCondition(shop);
    }
    /**
     * 根据ID获取店铺详情。
     */
    @GetMapping("/getShopById/{shopId}")
    public ShopVO getShopById(@PathVariable("shopId") Long shopId ){
        return shopService.queryById(shopId);
    }
    /**
     * 根据ID集合批量查询店铺。
     */
    @GetMapping("/shopListByIds")
      public List<ShopVO> listShopByIds(@RequestParam("shopIdList") List<Long> shopIdList){
        return shopService.getShopList(shopIdList);
    }
    /**
     * 获取店铺总数。
     */
    @GetMapping("/getShopTotal")
    public Integer getShopTotal() {
        return shopService.getShopTotal();
    }
    /**
     * 获取最近新增的店铺。
     */
    @GetMapping("/getRecentShops")
    public List<ShopVO> getRecentShops(@RequestParam("limit") Integer limit){
        return shopService.getRecentShops(limit);
    }
   /**
    * 批量更新店铺评价数。
    */
    @PostMapping("/updateReviewCountBatch")
   public Boolean updateReviewCountBatch(@RequestBody Map<Long, Integer> updateMap){
        return shopService.updateReviewCountBatch(updateMap);
    }
   /**
    * 批量更新店铺收藏数。
    */
    @PostMapping("/updateStarCountBatch")
   public Boolean updateStarCountBatch(@RequestBody Map<Long, Integer> updateMap){
        return shopService.updateStarCountBatch(updateMap);
    }
   /**
    * 批量更新店铺粉丝数。
    */
    @PostMapping("/updateFansCountBatch")
   public Boolean updateFansCountBatch(@RequestBody Map<Long, Integer> updateMap){
        return shopService.updateFansCountBatch(updateMap);
    }

    /**
     * 获取店铺收藏数。
     */
    @GetMapping("/getStarCount/{sourceId}")
    public Integer getStarCount(@PathVariable("sourceId") Long sourceId) {
        if (sourceId == null) {
            return 0;
        }
        String starCountKey = StarTypeEnum.SHOP_STAR.getStarCountKeyPrefix() + sourceId;
        Integer starCount = redisService.getCacheObject(starCountKey);
        if (starCount != null) {
            return starCount;
        }
        Shop shop = shopService.getById(sourceId);
        starCount = shop != null && shop.getStared() != null ? shop.getStared() : 0;
        redisService.setCacheObject(starCountKey, starCount);
        return starCount;
    }

    /**
     * 更新店铺状态。
     */
    @PostMapping("/updateShopStatus")
    Boolean updateShopStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status, @RequestParam(value = "reason", required = false) String reason){
        return shopService.updateShopStatus(id, status, reason);
    }

    /**
     * 获取全部店铺ID列表。
     */
    @GetMapping("/getAllShopIds")
    public List<Long> getAllShopIds() {
        return shopService.list().stream()
                .map(Shop::getId)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 批量更新店铺销量。
     */
    @PostMapping("/updateSoldBatch")
    public Boolean updateSoldBatch(@RequestBody Map<Long, Integer> updateMap){
        return shopService.updateSoldBatch(updateMap);
    }

    /**
     * 获取店铺销量。
     */
    @GetMapping("/getSold/{id}")
    public Integer getSold(@PathVariable("id") Long id){
        if (id == null) {
            return 0;
        }
        String soldCountKey = SalesTypeEnum.SHOP_SALES.getCountKeyPrefix() + id;
        Integer sold = redisService.getCacheObject(soldCountKey);
        if (sold != null) {
            return sold;
        }
        Shop shop = shopService.getById(id);
        sold = shop != null && shop.getSold() != null ? shop.getSold() : 0;
        redisService.setCacheObject(soldCountKey, sold);
        return sold;
    }
}
