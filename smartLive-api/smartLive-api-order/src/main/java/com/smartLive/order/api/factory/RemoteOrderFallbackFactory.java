package com.smartLive.order.api.factory;

import com.smartLive.common.core.domain.R;
import com.smartLive.order.api.RemoteOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
@Component
@Slf4j
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
            public Integer getOrderCount(Long userId) {
                log.error("获取订单数量失败:{}", cause.getMessage());
                return 0;
            }
            /**
             * 获取订单总数
             * @return
             */
            @Override
            public Integer getOrderTotal() {
                log.error("获取订单总数失败:{}", cause.getMessage());
                return 0;
            }
        };

    }
}
