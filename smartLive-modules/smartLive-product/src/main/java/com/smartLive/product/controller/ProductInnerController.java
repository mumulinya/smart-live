package com.smartLive.product.controller;

import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.product.domain.Product;
import com.smartLive.product.service.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商品内部 Controller。
 */
@RestController
@RequestMapping("/inner/product")
public class ProductInnerController extends BaseController {

    @Autowired
    private IProductService productService;

    /**
     * 购买商品（统一入口）。
     */
    @PostMapping("/purchase")
    public Long purchaseProduct(@RequestParam("id") Long productId, @RequestParam("userId") Long userId) {
        return productService.purchaseProduct(productId, userId);
    }

    /**
     * 扣减库存（OrderService 调用）。
     */
    @PostMapping("/deductStock/{id}")
    public Boolean deductStock(@PathVariable("id") Long productId) {
        return productService.deductStock(productId);
    }

    /**
     * 恢复库存。
     */
    @PostMapping("/recoverStock/{id}")
    public Boolean recoverStock(@PathVariable("id") Long productId) {
        return productService.recoverStock(productId);
    }

    /**
     * 获取商品列表（内部）。
     */
    @PostMapping("/listProduct")
    public List<Product> listProduct(@RequestBody Product product) {
        return productService.selectProductEntityList(product);
    }

    @GetMapping("/listAllProduct")
    public List<Product> listAllProduct() {
        return productService.listProduct();
    }

    /**
     * 获取商品总数。
     */
    @GetMapping("/total")
    public Integer getProductTotal() {
        return productService.getProductTotal();
    }

    /**
     * 根据 ID 列表获取商品。
     */
    @GetMapping("/getProductListByIds")
    public List<Product> getProductListByIds(@RequestParam("sourceIdList") List<Long> sourceIdList) {
        return productService.getProductListByIds(sourceIdList);
    }

    /**
     * 根据 ID 获取商品。
     */
    @GetMapping("/{id}")
    public Product getProductById(@PathVariable("id") Long productId) {
        return productService.selectProductEntityById(productId);
    }

    /**
     * 批量更新商品收藏数。
     */
    @PostMapping("/updateStarCountBatch")
    Boolean updateStarCountBatch(@RequestBody Map<Long, Integer> updateMap) {
        return productService.updateStarCountBatch(updateMap);
    }

    /**
     * 批量更新商品粉丝数。
     */
    @PostMapping("/updateFansCountBatch")
    Boolean updateFansCountBatch(@RequestBody Map<Long, Integer> updateMap) {
        return productService.updateFansCountBatch(updateMap);
    }

    /**
     * 获取商品收藏数。
     */
    @GetMapping("/getProductStarCount")
    Integer getProductStarCount(@RequestParam("sourceId") Long sourceId) {
        return productService.getProductStarCount(sourceId);
    }

    /**
     * 批量更新商品评价数。
     */
    @PostMapping("/updateReviewCountBatch")
    Boolean updateReviewCountBatch(@RequestBody Map<Long, Integer> updateMap) {
        return productService.updateReviewCountBatch(updateMap);
    }

    /**
     * 更新商品状态。
     */
    @PostMapping("/updateProductStatus")
    Boolean updateProductStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        return productService.updateProductStatus(id, status);
    }
}
