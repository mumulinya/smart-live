package com.smartLive.marketing.api.factory;

import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.marketing.api.RemoteMarketingService;
import com.smartLive.marketing.api.dto.VoucherDTO;
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

            @Override
            public Result getVoucherById(Long voucherId) {
                return Result.fail("查询优惠券失败");
            }
        };
    }
}
