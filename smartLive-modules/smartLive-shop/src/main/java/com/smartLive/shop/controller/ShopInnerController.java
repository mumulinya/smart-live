package com.smartLive.shop.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.utils.poi.ExcelUtil;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.common.core.web.page.TableDataInfo;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
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
 * 店铺服务内部Controller
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@RestController
@RequestMapping("/inner/shop")
public class ShopInnerController extends BaseController {
    @Autowired
    private IShopService shopService;

    @GetMapping("/{shopName}")
    public ShopVO getShopByShopName(@PathVariable("shopName") String shopName){
        return shopService.getShopByShopName(shopName);
    }

    /**
     * 根据条件查询商家信息
     */
    @PostMapping("/getShopList")
    public List<ShopVO> getShopByCondition(@RequestBody Shop shop){
        return shopService.getShopByCondition(shop);
    }
    /**
     * 根据id查询商家信息
     */
    @GetMapping("/getShopById/{shopId}")
    public ShopVO getShopById(@PathVariable("shopId") Long shopId ){
        return shopService.queryById(shopId);
    }
    /**
     * 根据id列表查询商家信息
     */
    @GetMapping("/shopListByIds")
     public List<ShopVO> listShopByIds(@RequestParam("shopIdList") List<Long> shopIdList){
        return shopService.getShopList(shopIdList);
    }
    /**
     * 获取商家总数
     */
    @GetMapping("/getShopTotal")
    public Integer getShopTotal() {
        return shopService.getShopTotal();
    }
    /**
     * 获取最近创建商家
     */
    @GetMapping("/getRecentShops")
    public List<ShopVO> getRecentShops(@RequestParam("limit") Integer limit){
        return shopService.getRecentShops(limit);
    }
    /**
     * 批量更新商家评论数
     */
    @PostMapping("/updateReviewCountBatch")
   public Boolean updateReviewCountBatch(@RequestBody Map<Long, Integer> updateMap){
        return shopService.updateReviewCountBatch(updateMap);
    }
    /**
     * 批量更新商家收藏数
     */
    @PostMapping("/updateStarCountBatch")
   public Boolean updateStarCountBatch(@RequestBody Map<Long, Integer> updateMap){
        return shopService.updateStarCountBatch(updateMap);
    }
    /**
     * Batch update shop fans count.
     */
    @PostMapping("/updateFansCountBatch")
   public Boolean updateFansCountBatch(@RequestBody Map<Long, Integer> updateMap){
        return shopService.updateFansCountBatch(updateMap);
    }

    /**
     * 更新店铺状态
     */
    @PostMapping("/updateShopStatus")
    Boolean updateShopStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status, @RequestParam(value = "reason", required = false) String reason){
        return shopService.updateShopStatus(id, status, reason);
    }

    /**
     * 获取全部店铺ID列表（供热榜全量重建使用）
     */
    @GetMapping("/getAllShopIds")
    public List<Long> getAllShopIds() {
        return shopService.list().stream()
                .map(Shop::getId)
                .collect(java.util.stream.Collectors.toList());
    }
}
