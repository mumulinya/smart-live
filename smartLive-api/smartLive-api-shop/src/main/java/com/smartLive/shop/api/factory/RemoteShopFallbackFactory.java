package com.smartLive.shop.api.factory;

import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.shop.api.DTO.ShopAnalysisDTO;
import com.smartLive.shop.api.DTO.ShopDTO;
import com.smartLive.shop.api.DTO.ShopSuggestDTO;
import com.smartLive.shop.api.DTO.ShopTypeDTO;
import com.smartLive.shop.api.RemoteShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class RemoteShopFallbackFactory implements FallbackFactory<RemoteShopService> {

    @Override
    public RemoteShopService create(Throwable cause) {
        return new RemoteShopService() {
            @Override
            public ShopDTO getShopByShopName(String shopName) {
                log.error("Get shop by name failed: {}", cause.getMessage());
                return null;
            }

            @Override
            public List<ShopDTO> queryShopList(ShopDTO shopDTo) {
                log.error("Query shop list failed: {}", cause.getMessage());
                return Collections.emptyList();
            }

            @Override
            public List<ShopTypeDTO> getShopTypeList() {
                log.error("Get shop types failed: {}", cause.getMessage());
                return Collections.emptyList();
            }

            @Override
            public ShopDTO getShopById(Long shopId) {
                log.error("Get shop by id failed: {}", cause.getMessage());
                return null;
            }

            @Override
            public List<ShopDTO> getShopList(List<Long> shopIdList) {
                log.error("Get shop list by ids failed: {}", cause.getMessage());
                return Collections.emptyList();
            }

            @Override
            public Integer getShopTotal() {
                log.error("Get shop total failed: {}", cause.getMessage());
                return 0;
            }

            @Override
            public List<ShopDTO> getRecentShops(Integer limit) {
                log.error("Get recent shops failed: {}", cause.getMessage());
                return Collections.emptyList();
            }

            @Override
            public Boolean updateReviewCountBatch(Map<Long, Integer> updateMap) {
                log.error("Update review count batch failed: {}", cause.getMessage());
                return false;
            }

            @Override
            public Boolean updateStarCountBatch(Map<Long, Integer> updateMap) {
                log.error("Update star count batch failed: {}", cause.getMessage());
                return false;
            }

            @Override
            public Boolean updateFansCountBatch(Map<Long, Integer> updateMap) {
                log.error("Update fans count batch failed: {}", cause.getMessage());
                return false;
            }

            @Override
            public Integer getStarCount(Long sourceId) {
                log.error("Get shop star count failed: {}", cause.getMessage());
                return 0;
            }

            @Override
            public Boolean updateShopStatus(Long id, Integer status, String reason) {
                log.error("Update shop status failed: {}", cause.getMessage());
                return false;
            }

            @Override
            public List<Long> getAllShopIds() {
                log.error("Get all shop ids failed: {}", cause.getMessage());
                return Collections.emptyList();
            }

            @Override
            public Boolean updateSoldBatch(Map<Long, Integer> updateMap) {
                log.error("Update sold batch failed: {}", cause.getMessage());
                return false;
            }

            @Override
            public Integer getSold(Long id) {
                log.error("Get sold failed: {}", cause.getMessage());
                return 0;
            }

            @Override
            public ShopAnalysisDTO getShopAnalysis(Long shopId, String timeRange) {
                log.error("Get shop analysis failed: {}", cause.getMessage());
                log.info("timeRange: {}", timeRange);
                return new ShopAnalysisDTO();
            }

            @Override
            public ShopSuggestDTO getShopSuggest(Long shopId, String timeRange) {
                log.error("Get shop suggest failed: {}", cause.getMessage());
                return new ShopSuggestDTO();
            }
        };
    }
}