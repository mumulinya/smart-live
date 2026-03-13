package com.smartLive.order.api.factory;

import com.smartLive.common.core.domain.R;
import com.smartLive.order.api.DTO.ProductSoldDTO;
import com.smartLive.order.api.RemoteOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

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

            @Override
            public Integer updateOrderReviewStatus(Long orderId, Long reviewId, java.util.Date reviewTime) {
                log.error("修改订单评论状态失败:{}", cause.getMessage());
                return 0;
            }

            @Override
            public Integer paySuccess(Long orderId, Integer payType) {
                log.error("更新订单支付状态失败:{}", cause.getMessage());
                return 0;
            }

            /**
             * 统计商品销售数量
             *
             * @return
             */
            @Override
            public List<ProductSoldDTO> countProductSold() {
                return List.of();
            }

            @Override
            public Integer countWeekOrders(Long shopId) {
                log.error("统计店铺近7天核销订单数失败:{}", cause.getMessage());
                return 0;
            }
        };

    }
}
