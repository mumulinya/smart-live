package com.smartLive.blog.api;

import com.smartLive.common.core.constant.ServiceNameConstants;

import com.smartLive.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;


@FeignClient(contextId = "remoteBlogService", value = ServiceNameConstants.BLOG_SERVICE)
public interface RemoteBlogService {

    /**
     * 更新博客评论数
     * @param blogId
     * @return
     */
    @PostMapping("/blog/updateCommentById/{id}")
    public R<Boolean> updateCommentById(@PathVariable("id") Long blogId);
}
