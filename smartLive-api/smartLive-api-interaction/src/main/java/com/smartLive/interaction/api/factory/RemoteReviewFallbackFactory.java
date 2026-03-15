package com.smartLive.interaction.api.factory;

import com.smartLive.interaction.api.DTO.ReviewDTO;
import com.smartLive.interaction.api.DTO.ShopReviewAnalysisDTO;
import com.smartLive.interaction.api.DTO.ShopReviewSuggestDTO;
import com.smartLive.interaction.api.RemoteReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class RemoteReviewFallbackFactory implements FallbackFactory<RemoteReviewService> {

    @Override
    public RemoteReviewService create(Throwable throwable) {
        return new RemoteReviewService() {
            @Override
            public Boolean isReview(ReviewDTO reviewDTO) {
                log.error("Check review status failed: {}", throwable.getMessage());
                return false;
            }

            @Override
            public Boolean updateReviewStatus(Long id, Integer status, String reason) {
                log.error("Update review status failed: {}", throwable.getMessage());
                return false;
            }

            @Override
            public Boolean saveAiCreateReview(List<ReviewDTO> reviews) {
                log.error("Save AI reviews failed: {}", throwable.getMessage());
                return false;
            }

            @Override
            public ShopReviewAnalysisDTO getShopReviewAnalysis(Long shopId, String startTime, String endTime) {
                log.error("Get shop review analysis failed: {}", throwable.getMessage());
                return new ShopReviewAnalysisDTO(BigDecimal.ZERO, 0);
            }

            @Override
            public ShopReviewSuggestDTO getShopReviewSuggest(Long shopId, String timeRange) {
                log.error("Get shop review suggest failed: {}", throwable.getMessage());
                return new ShopReviewSuggestDTO(BigDecimal.ZERO, 0, 0, new ArrayList<>());
            }

            @Override
            public ReviewDTO getReviewById(Long id) {
                log.error("Get review by id failed: {}", throwable.getMessage());
                return null;
            }
        };
    }
}