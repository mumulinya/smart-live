package com.smartLive.product.controller;

import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.product.domain.Product;
import com.smartLive.product.service.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商品内部Controller
 *
 * @author 桃桃
 * @date 2026-02-18
 */
@RestController
@RequestMapping("/inner/product")
public class ProductInnerController extends BaseController {

    @Autowired
    private IProductService productService;


    /**
     * 购买商品 (统一入口)
     */
    @PostMapping("/purchase")
    public Long purchaseProduct(@RequestParam("id") Long productId, @RequestParam("userId") Long userId) {
        return productService.purchaseProduct(productId, userId);
    }

    /**
     * 扣减库存 (OrderService调用)
     *
     * @param productId 商品id
     * @return
     */
    @PostMapping("/deductStock/{id}")
    public Boolean deductStock(@PathVariable("id") Long productId) {
        return productService.deductStock(productId);
    }

    /**
     * 恢复库存
     */
    @PostMapping("/recoverStock/{id}")
    public Boolean recoverStock(@PathVariable("id") Long productId){
        return productService.recoverStock(productId);
    }

    /**
     * 获取店铺的商品列表 (Internal check?)
     * Or maybe "listSeckillVoucher" was used for checking seckill items.
     * I'll keep generic list for now if needed.
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
     * 获取商品总数
     */
    @GetMapping("/total")
    public Integer getProductTotal(){
        return productService.getProductTotal();
    }

    /**
     * 获取商品列表
     */
    @GetMapping("/getProductListByIds")
    public List<Product> getProductListByIds(@RequestParam("sourceIdList") List<Long> sourceIdList){
        List<Product> list = productService.getProductListByIds(sourceIdList);
        return list;
    }

    /**
     * 获取商品信息
     */
    @GetMapping("/{id}")
    public Product getProductById(@PathVariable("id") Long productId){
        return productService.selectProductEntityById(productId);
    }

    /**
     * 批量收藏点赞数
     */
    @PostMapping("/updateStarCountBatch")
    Boolean updateStarCountBatch(@RequestBody Map<Long, Integer> updateMap){
        return productService.updateStarCountBatch(updateMap);
    }
    /**
     * 获取收藏数
     */
    @GetMapping("/getProductStarCount")
    Integer getProductStarCount(@RequestParam("sourceId") Long sourceId){
        return productService.getProductStarCount(sourceId);
    }
    /**
     * 批量更新评价数
     */
    @PostMapping("/updateReviewCountBatch")
    Boolean updateReviewCountBatch(@RequestBody Map<Long, Integer> updateMap){
        return productService.updateReviewCountBatch(updateMap);
    }

    /**
     * 更新商品状态
     * @param id
     * @param status
     * @return
     */
    @PostMapping("/updateProductStatus")
    Boolean updateProductStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status){
        return productService.updateProductStatus(id, status);
    }
}
