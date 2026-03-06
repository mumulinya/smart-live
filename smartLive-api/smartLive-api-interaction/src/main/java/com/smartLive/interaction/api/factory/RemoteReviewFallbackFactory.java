package com.smartLive.interaction.api.factory;

import com.smartLive.interaction.api.DTO.ReviewDTO;
import com.smartLive.interaction.api.RemoteReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class RemoteReviewFallbackFactory implements FallbackFactory<RemoteReviewService> {
    @Override
    public RemoteReviewService create(Throwable throwable) {
        return new RemoteReviewService() {
            @Override
            public Boolean isReview(ReviewDTO reviewDTO) {
                log.error("查询是否评价失败:{}", throwable.getMessage());
                return false;
            }

            @Override
            public Boolean updateReviewStatus(Long id, Integer status, String reason) {
                log.error("评价服务调用失败:{}", throwable.getMessage());
                return false;
            }

            /**
             * 保存AI创建的评价到Redis
             *
             * @param reviews 评价列表
             * @return 操作结果
             */
            @Override
            public Boolean saveAiCreateReview(List<ReviewDTO> reviews) {
                log.error("保存AI创建的评价到Redis失败:{}", throwable.getMessage());
                return false;
            }
        };
    }
}
