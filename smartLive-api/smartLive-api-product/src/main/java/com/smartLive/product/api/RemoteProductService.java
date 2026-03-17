package com.smartLive.product.api;

import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.product.api.DTO.ProductDTO;
import com.smartLive.product.api.factory.RemoteProductFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(
        contextId = "remoteProductService",
        value = ServiceNameConstants.PRODUCT_SERVICE,
        fallbackFactory = RemoteProductFallbackFactory.class
)
public interface RemoteProductService {

    @PostMapping("/inner/product/deductStock/{productId}")
    Boolean deductStock(@PathVariable("productId") Long productId);

    @GetMapping("/inner/product/getProductById/{productId}")
    ProductDTO getProductById(@PathVariable("productId") Long productId);

    @PostMapping("/inner/product/recoverStock")
    Boolean recoverStock(@RequestParam("productId") Long productId,
                         @RequestParam(value = "userId", required = false) Long userId);

    @PostMapping("/inner/product/recoverRedisStockAndEligibility")
    Boolean recoverRedisStockAndEligibility(@RequestParam("productId") Long productId,
                                            @RequestParam(value = "userId", required = false) Long userId);

    @GetMapping("/inner/product/getProductTotal")
    Integer getProductTotal();

    @PostMapping("/inner/product/purchaseProduct")
    Long purchaseProduct(@RequestParam("productId") Long productId,
                         @RequestParam("userId") Long userId);

    @GetMapping("/inner/product/getProductListByIds")
    List<ProductDTO> getProductListByIds(@RequestParam("sourceIdList") List<Long> sourceIdList);

    @PostMapping("/inner/product/updateStarCountBatch")
    Boolean updateStarCountBatch(@RequestBody Map<Long, Integer> updateMap);

    @PostMapping("/inner/product/updateFansCountBatch")
    Boolean updateFansCountBatch(@RequestBody Map<Long, Integer> updateMap);

    @GetMapping("/inner/product/getProductStarCount")
    Integer getProductStarCount(@RequestParam("sourceId") Long sourceId);

    @PostMapping("/inner/product/updateReviewCountBatch")
    Boolean updateReviewCountBatch(@RequestBody Map<Long, Integer> updateMap);

    @PostMapping("/inner/product/updateProductStatus")
    Boolean updateProductStatus(@RequestParam("id") Long id,
                                @RequestParam("status") Integer status,
                                @RequestParam(value = "reason", required = false) String reason);

    @PostMapping("/inner/product/updateSoldBatch")
    Boolean updateSoldBatch(@RequestBody Map<Long, Integer> updateMap);

    @GetMapping("/inner/product/getSold/{id}")
    Integer getSold(@PathVariable("id") Long id);

    @GetMapping("/inner/product/getAllProductIds")
    List<Long> getAllProductIds();

    @GetMapping("/inner/product/slow/{shopId}")
    List<ProductDTO> getShopSlowProducts(@PathVariable("shopId") Long shopId,
                                         @RequestParam(value = "limit", defaultValue = "3") Integer limit);
}
