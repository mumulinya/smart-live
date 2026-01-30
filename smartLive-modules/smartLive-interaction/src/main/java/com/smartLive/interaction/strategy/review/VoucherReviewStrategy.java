package com.smartLive.interaction.strategy.review;

import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.marketing.api.RemoteVoucherService;
import com.smartLive.shop.api.RemoteShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class VoucherReviewStrategy implements ReviewStrategy {

    private final RemoteVoucherService remoteVoucherService;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.VOUCHER_RESOURCE.getCode();
    }

    @Override
    public void transReviewCountFromRedis2DB(Map<Long, Integer> updateMap) {
        // 调用店铺服务的批量更新接口
        remoteVoucherService.updateReviewCountBatch(updateMap);
    }
}