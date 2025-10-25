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
            public R<VoucherDTO> getVoucherById(Long voucherId) {
                return R.fail("查询秒杀券失败");
            }

            /**
             * 恢复库存
             *
             * @param voucherId
             */
            @Override
            public R<Boolean> recoverVoucherStock(Long voucherId) {
                return R.fail("恢复库存失败");
            }
        };
    }
}
