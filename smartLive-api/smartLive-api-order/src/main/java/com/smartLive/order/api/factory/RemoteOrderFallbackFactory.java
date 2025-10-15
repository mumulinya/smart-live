package com.smartLive.order.api.factory;

import com.smartLive.common.core.domain.R;
import com.smartLive.order.api.RemoteOrderService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
@Component
public class RemoteOrderFallbackFactory implements FallbackFactory<RemoteOrderService> {
    @Override
    public RemoteOrderService create(Throwable cause) {
        return new RemoteOrderService() {
            /**
             * 获取订单数量
             * @param userId
             * @return
             */
            @Override
            public R<Integer> getOrderCount(Long userId) {
                return R.fail("查询订单数失败");
            }

        };

    }
}
