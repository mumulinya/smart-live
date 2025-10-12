package com.smartLive.comment.api;


import com.smartLive.comment.api.factory.RemoteCommentFallbackFactory;
import com.smartLive.common.core.constant.ServiceNameConstants;

import com.smartLive.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(contextId = "remoteCommentService", value = ServiceNameConstants.COMMENT_SERVICE, fallbackFactory = RemoteCommentFallbackFactory.class)
public interface RemoteCommentService {

    /**
     * 获取评论数量
     * @param userId
     * @return
     */
    @GetMapping("/comment/getCommentCount/{userId}")
    R<Integer> getCommentCount( @PathVariable("userId")Long userId);
}
