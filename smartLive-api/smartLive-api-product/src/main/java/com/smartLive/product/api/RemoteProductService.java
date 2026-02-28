package com.smartLive.product.api;

import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.product.api.DTO.ProductDTO;
import com.smartLive.product.api.factory.RemoteProductFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(contextId = "remoteProductService", value = ServiceNameConstants.PRODUCT_SERVICE, fallbackFactory = RemoteProductFallbackFactory.class)
public interface RemoteProductService {
    /**
     * 扣减库存
     */
    @PostMapping("/inner/product/deductStock/{id}")
    Boolean deductStock(@PathVariable("id") Long productId);

    @GetMapping("/inner/product/{id}")
    ProductDTO getProductById(@PathVariable("id") Long productId);

    /**
     * 恢复库存
     */
    @PostMapping("/inner/product/recoverStock/{id}")
    Boolean recoverStock(@PathVariable("id") Long productId);

    /**
     * 获取商品总数
     */
    @GetMapping("/inner/product/total")
    Integer getProductTotal();

    /**
     * 购买商品
     */
    @PostMapping("/inner/product/purchase")
    Long purchaseProduct(@RequestParam("id") Long productId, @RequestParam("userId") Long userId);

    /**
     * 获取商品列表
     */
    @GetMapping("/inner/product/getProductListByIds")
    List<ProductDTO> getProductListByIds(@RequestParam("sourceIdList") List<Long> sourceIdList);

    /**
     * 批量更新收藏数
     */
    @PostMapping("/inner/product/updateStarCountBatch")
    Boolean updateStarCountBatch(@RequestBody Map<Long, Integer> updateMap);

    /**
     * Batch update product fans count.
     */
    @PostMapping("/inner/product/updateFansCountBatch")
    Boolean updateFansCountBatch(@RequestBody Map<Long, Integer> updateMap);

     /**
     * 获取收藏数
     */
     @GetMapping("/inner/product/getProductStarCount")
    Integer getProductStarCount(@RequestParam("sourceId") Long sourceId);

     /**
     * 批量更新评价数
     */
     @PostMapping("/inner/product/updateReviewCountBatch")
    Boolean updateReviewCountBatch(@RequestBody Map<Long, Integer> updateMap);

    /**
     * 更新商品状态
     */
    @PostMapping("/inner/product/updateProductStatus")
    Boolean updateProductStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status);
}
