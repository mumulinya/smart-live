package com.smartLive.order.api.factory;

import com.smartLive.order.api.DTO.ProductSoldDTO;
import com.smartLive.order.api.DTO.ShopOrderAnalysisDTO;
import com.smartLive.order.api.DTO.ShopOrderSuggestDTO;
import com.smartLive.order.api.RemoteOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class RemoteOrderFallbackFactory implements FallbackFactory<RemoteOrderService> {
    @Override
    public RemoteOrderService create(Throwable cause) {
        return new RemoteOrderService() {
            @Override
            public Integer getOrderCount(Long userId) {
                log.error("get order count failed: {}", cause.getMessage());
                return 0;
            }

            @Override
            public Integer getOrderTotal() {
                log.error("get order total failed: {}", cause.getMessage());
                return 0;
            }

            @Override
            public Integer updateOrderReviewStatus(Long orderId, Long reviewId, Date reviewTime) {
                log.error("update order review status failed: {}", cause.getMessage());
                return 0;
            }

            @Override
            public Integer paySuccess(Long orderId, Integer payType) {
                log.error("pay success callback failed: {}", cause.getMessage());
                return 0;
            }

            @Override
            public List<ProductSoldDTO> countProductSold() {
                return List.of();
            }

            @Override
            public Integer countWeekOrders(Long shopId) {
                log.error("count week orders failed: {}", cause.getMessage());
                return 0;
            }

            @Override
            public ShopOrderAnalysisDTO getShopOrderAnalysis(Long shopId, String startTime, String endTime) {
                log.error("get shop order analysis failed: {}", cause.getMessage());
                return new ShopOrderAnalysisDTO(0, BigDecimal.ZERO, 0, new ArrayList<>());
            }

            @Override
            public ShopOrderSuggestDTO getShopOrderSuggest(Long shopId, String timeRange) {
                log.error("get shop order suggest failed: {}", cause.getMessage());
                return new ShopOrderSuggestDTO(0, new ArrayList<>(), new ArrayList<>());
            }
        };
    }
}