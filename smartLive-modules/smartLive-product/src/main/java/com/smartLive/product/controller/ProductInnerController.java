package com.smartLive.product.controller;

import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.product.domain.Product;
import com.smartLive.product.service.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inner/product")
public class ProductInnerController extends BaseController {

    @Autowired
    private IProductService productService;

    @GetMapping("/{id}")
    public Product getProductEntityById(@PathVariable("id") Long productId) {
        return productService.selectProductEntityById(productId);
    }

    @GetMapping("/getProductById/{id}")
    public Product getProductById(@PathVariable("id") Long productId) {
        return productService.selectProductEntityById(productId);
    }

    @PostMapping("/deductStock/{productId}")
    public Boolean deductStock(@PathVariable("productId") Long productId) {
        return productService.deductStock(productId);
    }

    @PostMapping("/recoverStock")
    public Boolean recoverStock(@RequestParam("productId") Long productId,
                                @RequestParam(value = "userId", required = false) Long userId) {
        return productService.recoverStock(productId, userId);
    }

    @PostMapping("/recoverRedisStockAndEligibility")
    public Boolean recoverRedisStockAndEligibility(@RequestParam("productId") Long productId,
                                                   @RequestParam(value = "userId", required = false) Long userId) {
        return productService.recoverRedisStockAndEligibility(productId, userId);
    }

    @GetMapping("/getProductTotal")
    public Integer getProductTotal() {
        return productService.getProductTotal();
    }

    @PostMapping("/purchaseProduct")
    public Long purchaseProduct(@RequestParam("productId") Long productId,
                                @RequestParam("userId") Long userId) {
        return productService.purchaseProduct(productId, userId);
    }

    @GetMapping("/getProductListByIds")
    public List<Product> getProductListByIds(@RequestParam("sourceIdList") List<Long> sourceIdList) {
        return productService.getProductListByIds(sourceIdList);
    }

    @PostMapping("/updateStarCountBatch")
    public Boolean updateStarCountBatch(@RequestBody Map<Long, Integer> updateMap) {
        return productService.updateStarCountBatch(updateMap);
    }

    @PostMapping("/updateFansCountBatch")
    public Boolean updateFansCountBatch(@RequestBody Map<Long, Integer> updateMap) {
        return productService.updateFansCountBatch(updateMap);
    }

    @GetMapping("/getProductStarCount")
    public Integer getProductStarCount(@RequestParam("sourceId") Long sourceId) {
        return productService.getProductStarCount(sourceId);
    }

    @PostMapping("/updateReviewCountBatch")
    public Boolean updateReviewCountBatch(@RequestBody Map<Long, Integer> updateMap) {
        return productService.updateReviewCountBatch(updateMap);
    }

    @PostMapping("/updateProductStatus")
    public Boolean updateProductStatus(@RequestParam("id") Long id,
                                       @RequestParam("status") Integer status,
                                       @RequestParam(value = "reason", required = false) String reason) {
        return productService.updateProductStatus(id, status, reason);
    }

    @PostMapping("/updateSoldBatch")
    public Boolean updateSoldBatch(@RequestBody Map<Long, Integer> updateMap) {
        return productService.updateSoldBatch(updateMap);
    }

    @GetMapping("/getSold/{id}")
    public Integer getSold(@PathVariable("id") Long id) {
        Product product = productService.selectProductEntityById(id);
        return product != null ? product.getSold() : 0;
    }

    @GetMapping("/getAllProductIds")
    public List<Long> getAllProductIds() {
        return productService.list().stream()
                .map(Product::getId)
                .collect(Collectors.toList());
    }

    @GetMapping("/slow/{shopId}")
    public List<Product> getShopSlowProducts(@PathVariable("shopId") Long shopId,
                                             @RequestParam(value = "limit", defaultValue = "3") Integer limit) {
        return productService.getShopSlowProducts(shopId, limit);
    }
}
