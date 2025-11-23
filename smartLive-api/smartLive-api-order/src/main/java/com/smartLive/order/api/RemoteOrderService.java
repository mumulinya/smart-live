package com.smartLive.order.api;
import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.common.core.domain.R;
import com.smartLive.order.api.factory.RemoteOrderFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(contextId = "remoteOrderService", value = ServiceNameConstants.ORDER_SERVICE, fallbackFactory = RemoteOrderFallbackFactory.class)
public interface RemoteOrderService {

    /**
     * 获取订单数量
     * @param userId
     * @return
     */
    @GetMapping("/voucher-order/getOrderCount/{userId}")
    R<Integer> getOrderCount( @PathVariable("userId")Long userId);
    /**
     * 获取订单总数
     * @return
     */
    @GetMapping("/voucher-order/getOrderTotal")
    R<Integer> getOrderTotal();
}
