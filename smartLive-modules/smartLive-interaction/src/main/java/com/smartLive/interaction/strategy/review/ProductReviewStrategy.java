package com.smartLive.interaction.strategy.review;

import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.product.api.RemoteProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class ProductReviewStrategy implements ReviewStrategy {

    private final RemoteProductService remoteProductService;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.PRODUCT_RESOURCE.getCode();
    }

    @Override
    public void transReviewCountFromRedis2DB(Map<Long, Integer> updateMap) {
        // 调用商品服务的批量更新接口
        remoteProductService.updateReviewCountBatch(updateMap);
    }
}