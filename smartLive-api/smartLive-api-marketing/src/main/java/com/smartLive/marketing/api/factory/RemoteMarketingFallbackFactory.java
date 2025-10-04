package com.smartLive.marketing.api.factory;

import com.smartLive.common.core.domain.R;
import com.smartLive.marketing.api.RemoteMarketingService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class RemoteMarketingFallbackFactory implements FallbackFactory<RemoteMarketingService> {
    @Override
    public RemoteMarketingService create(Throwable throwable) {
        return new RemoteMarketingService() {
            @Override
            public R<Boolean> updateVoucherById(Long id) {
                return R.fail("秒杀券购买失败");
            }
        };
    }
}
