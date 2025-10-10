package com.smartLive.blog.api;

import com.smartLive.blog.api.dto.BlogDto;
import com.smartLive.blog.api.factory.RemoteBlogFallbackFactory;
import com.smartLive.common.core.constant.ServiceNameConstants;

import com.smartLive.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;


@FeignClient(contextId = "remoteBlogService", value = ServiceNameConstants.BLOG_SERVICE, fallbackFactory = RemoteBlogFallbackFactory.class)
public interface RemoteBlogService {

    /**
     * 更新博客评论数
     * @param blogId
     * @return
     */
    @PostMapping("/blog/updateCommentById/{id}")
    public R<Boolean> updateCommentById(@PathVariable("id") Long blogId);

    @GetMapping("/blog/getBlogById/{id}")
    R<BlogDto> getBlogById( @PathVariable("id")Long id);
}
