package com.smartLive.product.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.product.domain.VO.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import com.smartLive.product.domain.Product;
import com.smartLive.product.service.IProductService;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.utils.poi.ExcelUtil;
import com.smartLive.common.core.web.page.TableDataInfo;

@RestController
@RequestMapping("/product")
public class ProductController extends BaseController {
    @Autowired
    private IProductService productService;
    /**
     * 分页查询商品列表
     */

    @RequiresPermissions("business:product:list")
    @GetMapping("/list")
    public TableDataInfo list(Product product) {
        startPage();
        List<ProductVO> list = productService.selectProductList(product);
        return getDataTable(list);
    }
    /**
     * 查询商品列表详细信息
     */

    @GetMapping("/productList")
    public AjaxResult productList(Product product) {
        List<ProductVO> list = productService.selectProductList(product);
        return success(list);
    }
    /**
     * 导出商品列表
     */

    @RequiresPermissions("business:product:export")
    @Log(title = "product", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Product product) {
        List<Product> list = productService.selectProductEntityList(product);
        ExcelUtil<Product> util = new ExcelUtil<Product>(Product.class);
        util.exportExcel(response, list, "product list");
    }
    /**
     * 获取商品详细信息
     */

    @RequiresPermissions("business:product:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(productService.selectProductById(id));
    }
    /**
     * 新增商品
     */

    @RequiresPermissions("business:product:add")
    @Log(title = "product", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Product product) {
        return toAjax(productService.insertProduct(product));
    }
    /**
     * 修改商品信息
     */

    @RequiresPermissions("business:product:edit")
    @Log(title = "product", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Product product) {
        return toAjax(productService.updateProduct(product));
    }
    /**
     * 增加库存并发送消息
     */

    @PostMapping("/addStock/{id}")
    public AjaxResult addStock(@PathVariable("id") Long id) {
        return toAjax(productService.addStock(id));
    }
    /**
     * 降价通知并发送消息
     */

    @PostMapping("/priceReduced/{id}")
    public AjaxResult priceReduced(@PathVariable("id") Long id) {
        return toAjax(productService.priceReduced(id));
    }

    @PostMapping("/updateProductStatus")
    public Boolean updateProductStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status, @RequestParam(value = "reason", required = false) String reason) {
        return productService.updateProductStatus(id, status, reason);
    }
    /**
     * 获取热门商品排行榜
     */

    @GetMapping("/hot/rank")
    public Result getHotProductRank(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "category") Integer category) {
        if (category == null || (category != 1 && category != 2)) {
            return Result.fail("非法查询条件，category 与 name 不能同时为空");
        }
        return Result.ok(productService.getHotProductRank(current, size, category));
    }
    /**
     * 删除商品
     */

    @RequiresPermissions("product:product:remove")
    @Log(title = "product", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        return toAjax(productService.deleteProductByIds(ids));
    }
    /**
     * 全量发布商品到搜索引擎
     */

    @PostMapping("/allPublish")
    public AjaxResult allPublish() {
        return success(productService.allPublish());
    }
    /**
     * 全量发布商品到搜索引擎
     */

    @PostMapping("/publish/{ids}")
    public AjaxResult allPublish(@PathVariable String[] ids) {
        return success(productService.publish(ids));
    }
    /**
     * 获取指定店铺的商品列表
     */

    @GetMapping("/listByShop")
    public Result queryProductOfShop(Product product) {
        List<ProductVO> productList = productService.queryProductOfShop(product);
        return Result.ok(productList);
    }
    /**
     * 根据ID获取商品详情(内部调用)
     */

    @GetMapping(value = "/getProductById/{id}")
    public Result getProductById(@PathVariable("id") Long id) {
        return Result.ok(productService.getProductById(id));
    }

    @GetMapping("/search")
    public Result searchProducts(@RequestParam(name = "keyword", required = false) String keyword) {
        return Result.ok(productService.searchProducts(keyword));
    }
    /**
     * 购买商品生成订单
     */

    @PostMapping("/purchase/{id}")
    public Result purchaseProduct(@PathVariable("id") Long productId) {
        Long userId = UserContextHolder.getUser().getId();
        Long orderId = productService.purchaseProduct(productId, userId);
        return Result.ok(orderId);
    }
}
