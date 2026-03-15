package com.smartLive.product.api.factory;

import com.smartLive.product.api.DTO.ProductDTO;
import com.smartLive.product.api.RemoteProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class RemoteProductFallbackFactory implements FallbackFactory<RemoteProductService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteProductFallbackFactory.class);

    @Override
    public RemoteProductService create(Throwable throwable) {
        log.error("Remote product service call failed: {}", throwable.getMessage());
        return new RemoteProductService() {
            @Override
            public Boolean deductStock(Long productId) {
                return false;
            }

            @Override
            public ProductDTO getProductById(Long productId) {
                return null;
            }

            @Override
            public Boolean recoverStock(Long productId, Long userId) {
                return false;
            }

            @Override
            public Boolean recoverRedisStockAndEligibility(Long productId, Long userId) {
                return false;
            }

            @Override
            public Integer getProductTotal() {
                return 0;
            }

            @Override
            public Long purchaseProduct(Long productId, Long userId) {
                return null;
            }

            @Override
            public List<ProductDTO> getProductListByIds(List<Long> sourceIdList) {
                return Collections.emptyList();
            }

            @Override
            public Boolean updateStarCountBatch(Map<Long, Integer> updateMap) {
                return false;
            }

            @Override
            public Boolean updateFansCountBatch(Map<Long, Integer> updateMap) {
                return false;
            }

            @Override
            public Integer getProductStarCount(Long sourceId) {
                return 0;
            }

            @Override
            public Boolean updateReviewCountBatch(Map<Long, Integer> updateMap) {
                return false;
            }

            @Override
            public Boolean updateProductStatus(Long id, Integer status, String reason) {
                return false;
            }

            @Override
            public Boolean updateSoldBatch(Map<Long, Integer> updateMap) {
                return false;
            }

            @Override
            public Integer getSold(Long id) {
                return 0;
            }

            @Override
            public List<Long> getAllProductIds() {
                return Collections.emptyList();
            }
        };
    }
}