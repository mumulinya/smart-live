package com.smartLive.product.api.factory;

import com.smartLive.product.api.DTO.ProductDTO;
import com.smartLive.product.api.RemoteProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 商品服务降级处理
 *
 * @author 桃桃
 * @date 2026-02-18
 */
@Component
public class RemoteProductFallbackFactory implements FallbackFactory<RemoteProductService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteProductFallbackFactory.class);

    @Override
    public RemoteProductService create(Throwable throwable)
    {
        log.error("商品服务调用失败:{}", throwable.getMessage());
        return new RemoteProductService()
        {
            @Override
            public Boolean deductStock(Long productId)
            {
                return false;
            }

            @Override
            public ProductDTO getProductById(Long productId)
            {
                return null;
            }

            @Override
            public Boolean recoverStock(Long productId)
            {
                return false;
            }

            @Override
            public Integer getProductTotal()
            {
                return 0;
            }

            @Override
            public Long purchaseProduct(Long productId, Long userId)
            {
                return null;
            }

            @Override
            public List<ProductDTO> getProductListByIds(List<Long> sourceIdList)
            {
                return null;
            }

            @Override
            public Boolean updateStarCountBatch(Map<Long, Integer> updateMap)
            {
                return false;
            }
            @Override
            public Integer getProductStarCount(Long sourceId)
            {
                return 0;
            }

            @Override
            public Boolean updateReviewCountBatch(Map<Long, Integer> updateMap)
            {
                return false;
            }

            @Override
            public Boolean updateProductStatus(Long id, Integer status) {
                return false;
            }
        };
    }
}
