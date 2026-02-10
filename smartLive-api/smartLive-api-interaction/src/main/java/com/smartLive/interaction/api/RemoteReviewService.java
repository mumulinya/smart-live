package com.smartLive.interaction.api;

import com.smartLive.common.core.constant.ServiceNameConstants;
import com.smartLive.interaction.api.factory.RemoteReviewFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(contextId = "remoteReviewService", value = ServiceNameConstants.INTERACTION_SERVICE, fallbackFactory = RemoteReviewFallbackFactory.class)
public interface RemoteReviewService {

    /**
     * 更新评价状态
     */
    @PostMapping("/inner/review/updateReviewStatus")
    Boolean updateReviewStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status);
}
