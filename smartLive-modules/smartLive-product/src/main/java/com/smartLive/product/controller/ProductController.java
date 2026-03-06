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

/**
 * 商品Controller
 *
 * @author 桃桃
 * @date 2026-02-18
 */
@RestController
@RequestMapping("/product")
public class ProductController extends BaseController {
    @Autowired
    private IProductService productService;

    /**
     * 查询商品列表 (Data Table)
     */
    @RequiresPermissions("product:product:list")
    @GetMapping("/list")
    public TableDataInfo list(Product product) {
        startPage();
        List<ProductVO> list = productService.selectProductList(product);
        return getDataTable(list);
    }

    /**
     * 查询商品列表 (Ajax Result)
     */
    @GetMapping("/productList")
    public AjaxResult productList(Product product) {
        List<ProductVO> list = productService.selectProductList(product);
        return success(list);
    }

    /**
     * 导出商品列表
     */
    @RequiresPermissions("product:product:export")
    @Log(title = "商品", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Product product) {
        List<Product> list = productService.selectProductEntityList(product);
        ExcelUtil<Product> util = new ExcelUtil<Product>(Product.class);
        util.exportExcel(response, list, "商品数据");
    }

    /**
     * 获取商品详细信息
     */
    @RequiresPermissions("product:product:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(productService.selectProductById(id));
    }

    /**
     * 新增商品
     */
    @RequiresPermissions("product:product:add")
    @Log(title = "商品", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Product product) {
        return toAjax(productService.insertProduct(product));
    }

    /**
     * 修改商品
     */
    @RequiresPermissions("product:product:edit")
    @Log(title = "商品", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Product product) {
        return toAjax(productService.updateProduct(product));
    }

    /**
     * 商品新增库存
     */
    @PostMapping("/addStock/{id}")
    public AjaxResult addStock(@PathVariable("id") Long id) {
        return toAjax(productService.addStock(id));
    }

    /**
     * 商品降价
     */
    @PostMapping("/priceReduced/{id}")
    public AjaxResult priceReduced(@PathVariable("id") Long id) {
        return toAjax(productService.priceReduced(id));
    }

    /**
     * 修改商品状态
     */
    @PostMapping("/updateProductStatus")
    public Boolean updateProductStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status, @RequestParam(value = "reason", required = false) String reason) {
        return productService.updateProductStatus(id, status, reason);
    }

    /**
     * 获取热门商品排行榜（按类别区分代金券/团购套餐）
     * 首页调用：传 current=1, size=10, category=1 或 2
     * 榜单页调用：传 current=n, size=10, category=1 或 2
     *
     * @param current  页码，默认 1
     * @param size     每页数量，默认 10
     * @param category 商品种类 (1:代金券, 2:团购套餐) 必传，参考 ProductEnum
     */
    @GetMapping("/hot/rank")
    public Result getHotProductRank(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "category") Integer category) {
        if (category == null || (category != 1 && category != 2)) {
            return Result.fail("获取商品排行失败：商品分类参数 category 不合法(应为1或2)");
        }
        return Result.ok(productService.getHotProductRank(current, size, category));
    }

    /**
     * 删除商品
     */
    @RequiresPermissions("product:product:remove")
    @Log(title = "商品", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        return toAjax(productService.deleteProductByIds(ids));
    }

    /**
     * 全量发布商品信息
     */
    @PostMapping("/allPublish")
    public AjaxResult allPublish() {
        return success(productService.allPublish());
    }

    /**
     * 发布商品信息
     */
    @PostMapping("/publish/{ids}")
    public AjaxResult allPublish(@PathVariable String[] ids) {
        return success(productService.publish(ids));
    }

    /**
     * 查询店铺的商品列表
     *
     * @param product
     * @return 商品列表
     */
    @GetMapping("/listByShop")
    public Result queryProductOfShop(Product product) {
        List<ProductVO> productList = productService.queryProductOfShop(product);
        return Result.ok(productList);
    }

    /**
     * 根据商品id获取商品
     * @param id
     * @return
     */
    @GetMapping(value = "/getProductById/{id}")
    public Result getProductById(@PathVariable("id") Long id) {
        return Result.ok(productService.getProductById(id));
    }

    /**
     * 购买商品 (整合了普通购买和秒杀)
     */
    @PostMapping("/purchase/{id}")
    public Result purchaseProduct(@PathVariable("id") Long productId) {
        //获取当前用户id
        Long userId = UserContextHolder.getUser().getId();
        Long orderId = productService.purchaseProduct(productId, userId);
        return Result.ok(orderId);
    }
}
