package com.smartLive.shop.controller;

import java.util.List;

import com.smartLive.common.core.context.SecurityContextHolder;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.shop.domain.VO.ShopAnalysisVO;
import com.smartLive.shop.domain.VO.ShopSuggestVO;
import com.smartLive.shop.domain.VO.ShopVO;
import jakarta.servlet.http.HttpServletResponse;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.enums.common.AuditStatusEnum;
import com.smartLive.common.core.web.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import com.smartLive.shop.domain.Shop;
import com.smartLive.shop.service.IShopService;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.utils.poi.ExcelUtil;
import com.smartLive.common.core.web.page.TableDataInfo;

/**
 * 店铺管理控制层
 * 提供店铺的 CRUD 维护、热门排行榜分页查询、地理位置搜索及索引全量发布功能。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@RestController

@RequestMapping("/shop")
public class ShopController extends BaseController {
    @Autowired
    private IShopService shopService;

    /**
     * 分页查询店铺列表 (后台管理场景)
     * 支持根据店铺名称、分类、地址等字段进行多条件模糊匹配。
     *
     * @param shop 店铺查询实体
     * @return 分页后的店铺数据表格
     */
    @RequiresPermissions("business:shop:list")
    @GetMapping("/list")
    public TableDataInfo list(Shop shop) {
        startPage();
        List<Shop> list = shopService.selectShopList(shop);
        return getDataTable(list);
    }

    /**
     * 查询店铺列表
     */
    @GetMapping("/shopList")
    public AjaxResult shopList(Shop shop) {
        List<Shop> list = shopService.selectShopList(shop);
        return success(list);
    }
    /**
     * 导出店铺列表
     */
    @RequiresPermissions("business:shop:export")
    @Log(title = "店铺", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Shop shop) {
        List<Shop> list = shopService.selectShopList(shop);
        ExcelUtil<Shop> util = new ExcelUtil<Shop>(Shop.class);
        util.exportExcel(response, list, "店铺数据");
    }

    /**
     * 获取店铺详细信息
     */
    @RequiresPermissions("business:shop:query")
//    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return success(shopService.selectShopById(id));
    }

    /**
     * 新增店铺
     */
    @RequiresPermissions("business:shop:add")
    @Log(title = "店铺", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Shop shop) {
        return toAjax(shopService.insertShop(shop));
    }

    /**
     * 修改店铺
     */
    @RequiresPermissions("business:shop:edit")
    @Log(title = "店铺", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Shop shop) {
        return toAjax(shopService.updateShop(shop));
    }

    /**
     * 删除店铺
     */
    @RequiresPermissions("business:shop:remove")
    @Log(title = "店铺", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(shopService.deleteShopByIds(ids));
    }

    /**
     * 根据商铺名称关键字分页查询商铺信息
     *
     * @param name    商铺名称关键字
     * @param area    区域关键字
     * @param current 页码
     * @return 商铺列表
     */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "area",required = false) String area,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据类型分页查询
        Page<Shop> page = shopService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .like(StrUtil.isNotBlank(area), "address", area)
                .eq("status", 1)
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 返回数据
        return Result.ok(page.getRecords());
    }

    /**
     * 刷新缓存
     */
    @GetMapping("/flushCache")
    public AjaxResult flushCache() {
        return success(shopService.flushCache());
    }

    /**
     * 全量发布店铺
     */
    @PostMapping("/allPublish")
    public AjaxResult allPublish() {
        return success(shopService.allPublish());
    }

    /**
     * 发布店铺
     */
    @PostMapping("/publish/{ids}")
    public AjaxResult allPublish(@PathVariable String[] ids) {
        return success(shopService.publish(ids));
    }
    /**
     * 获取热门店铺排行榜 (大一统分页接口)
     * 
     * 核心逻辑：
     * 1. 优先从 Redis 缓存中获取已按热度分排序的店铺序列。
     * 2. 若提供坐标 (x,y)，则联动地理位置距离进行权重微调。
     * 3. 首页推荐场景通常传 current=1, size=10。
     *
     * @param current 当前页码
     * @param size    每页数量
     * @param x       用户当前经度 (可选)
     * @param y       用户当前纬度 (可选)
     * @return 热门店铺 VO 列表
     */
    @GetMapping("/hot/rank")
    public Result getHotShopRank(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "x", required = false) Double x,
            @RequestParam(value = "y", required = false) Double y) {
        return Result.ok(shopService.getHotShopRank(current, size, x, y));
    }

    /**
     * 根据id查询商铺信息
     *
     * @param id 商铺id
     * @return 商铺详情数据
     */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        ShopVO shop = shopService.queryById(id);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    /**
     * 根据多个店铺 ID 批量查询店铺详情
     * 常用于购物车、收藏夹等需要展示多个店铺基础信息的场景。
     *
     * @param ids 逗号分隔的店铺 ID 字符串 (例如: "1,2,3")
     * @return 店铺详情 VO 列表
     */
    @GetMapping("/listByIds")
    public Result queryShopByIds(@RequestParam("ids") String ids) {
        if (StrUtil.isBlank(ids)) {
            return Result.ok(new java.util.ArrayList<>());
        }
        java.util.List<Long> idList = java.util.Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(java.util.stream.Collectors.toList());
        java.util.List<ShopVO> shops = shopService.getShopList(idList);
        if (shops == null) {
            return Result.ok(new java.util.ArrayList<>());
        }
        shops = shops.stream()
                .filter(shop -> shop != null
                        && java.util.Objects.equals(shop.getStatus(), 1)
                        && java.util.Objects.equals(shop.getAuditStatus(), AuditStatusEnum.PASS.getCode()))
                .collect(java.util.stream.Collectors.toList());
        return Result.ok(shops);
    }

    @GetMapping("/analysis/{shopId}")
    public AjaxResult getShopAnalysis(@PathVariable("shopId") Long shopId,
                                      @RequestParam(value = "timeRange", defaultValue = "week") String timeRange) {
        return success(shopService.getShopAnalysis(shopId, timeRange));
    }

    @GetMapping("/suggest/{shopId}")
    public AjaxResult getShopSuggest(@PathVariable("shopId") Long shopId) {
        return success(shopService.getShopSuggest(shopId));
    }
}