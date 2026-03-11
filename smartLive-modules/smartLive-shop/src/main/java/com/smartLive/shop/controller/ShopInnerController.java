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
 * 店铺服务内部 RPC 接口
 * 专门用于微服务集群内部其他模块（如订单、商品、互动、AI 模块）通过 Feign 进行调用的同步/查询接口。
 * 
 * @author smartLive
 * @date 2026-03-11
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
     * 根据 ID 获取店铺详情 (Feign 内部调用)
     *
     * @param shopId 店铺 ID
     * @return 店铺 VO
     */
    @GetMapping("/getShopById/{shopId}")
    public ShopVO getShopById(@PathVariable("shopId") Long shopId ){
        return shopService.queryById(shopId);
    }
    /**
     * 根据多个 ID 批量获取店铺详情列表 (Feign 内部调用)
     *
     * @param shopIdList 店铺 ID 集合
     * @return 店铺 VO 列表
     */
    @GetMapping("/shopListByIds")
     public List<ShopVO> listShopByIds(@RequestParam("shopIdList") List<Long> shopIdList){
        return shopService.getShopList(shopIdList);
    }
    /**
     * 获取店铺总数 (Feign 内部调用)
     *
     * @return 店铺总数
     */
    @GetMapping("/getShopTotal")
    public Integer getShopTotal() {
        return shopService.getShopTotal();
    }
    /**
     * 获取最近创建的店铺列表 (Feign 内部调用)
     *
     * @param limit 限制数量
     * @return 店铺 VO 列表
     */
    @GetMapping("/getRecentShops")
    public List<ShopVO> getRecentShops(@RequestParam("limit") Integer limit){
        return shopService.getRecentShops(limit);
    }
    /**
     * 批量更新店铺评论数 (Feign 内部调用)
     *
     * @param updateMap 包含店铺 ID 和评论数增量的 Map
     * @return 更新是否成功
     */
    @PostMapping("/updateReviewCountBatch")
   public Boolean updateReviewCountBatch(@RequestBody Map<Long, Integer> updateMap){
        return shopService.updateReviewCountBatch(updateMap);
    }
    /**
     * 批量更新店铺收藏数 (Feign 内部调用)
     *
     * @param updateMap 包含店铺 ID 和收藏数增量的 Map
     * @return 更新是否成功
     */
    @PostMapping("/updateStarCountBatch")
   public Boolean updateStarCountBatch(@RequestBody Map<Long, Integer> updateMap){
        return shopService.updateStarCountBatch(updateMap);
    }
    /**
     * 批量更新店铺粉丝数 (Feign 内部调用)
     *
     * @param updateMap 包含店铺 ID 和粉丝数增量的 Map
     * @return 更新是否成功
     */
    @PostMapping("/updateFansCountBatch")
   public Boolean updateFansCountBatch(@RequestBody Map<Long, Integer> updateMap){
        return shopService.updateFansCountBatch(updateMap);
    }

    /**
     * 更新店铺状态 (Feign 内部调用)
     *
     * @param id 店铺 ID
     * @param status 新状态
     * @param reason 更新原因 (可选)
     * @return 更新是否成功
     */
    @PostMapping("/updateShopStatus")
    Boolean updateShopStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status, @RequestParam(value = "reason", required = false) String reason){
        return shopService.updateShopStatus(id, status, reason);
    }

    /**
     * 获取全部店铺 ID 列表 (热榜全量重建或搜索引擎增量统计使用)
     *
     * @return 所有有效的店铺 ID 集合
     */
    @GetMapping("/getAllShopIds")
    public List<Long> getAllShopIds() {
        return shopService.list().stream()
                .map(Shop::getId)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 批量更新销量 (Feign 内部调用)
     *
     * @param updateMap 包含店铺 ID 和销量增量的 Map
     * @return 更新是否成功
     */
    @PostMapping("/updateSoldBatch")
    public Boolean updateSoldBatch(@RequestBody Map<Long, Integer> updateMap){
        return shopService.updateSoldBatch(updateMap);
    }

    /**
     * 获取指定 ID 的销量 (Feign 内部调用)
     *
     * @param id 店铺 ID
     * @return 店铺销量，如果店铺不存在则返回 0
     */
    @GetMapping("/getSold/{id}")
    public Integer getSold(@PathVariable("id") Long id){
        Shop shop = shopService.getById(id);
        return shop != null ? shop.getSold() : 0;
    }
}
