package com.smartLive.blog.api;

import com.smartLive.blog.api.dto.BlogDto;
import com.smartLive.blog.api.factory.RemoteBlogFallbackFactory;
import com.smartLive.common.core.constant.ServiceNameConstants;

import com.smartLive.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;


@FeignClient(contextId = "remoteBlogService", value = ServiceNameConstants.BLOG_SERVICE, fallbackFactory = RemoteBlogFallbackFactory.class)
public interface RemoteBlogService {

    /**
     * 更新博客评论数
     * @param blogId
     * @return
     */
    @PostMapping("/blog/updateCommentById/{id}")
    public R<Boolean> updateCommentById(@PathVariable("id") Long blogId);

    /**
     * 获取博客详情
     * @param id
     * @return
     */
    @GetMapping("/blog/getBlogById/{id}")
    R<BlogDto> getBlogById( @PathVariable("id")Long id);

    /**
     * 获取博客数
     * @param userId
     * @return
     */
    @GetMapping("/blog/getBlogCount/{userId}")
    R<Integer> getBlogCount(@PathVariable("userId")Long userId);

    /**
     * 获取博客点赞数
     * @param userId
     * @return
     */
    @GetMapping("/blog/getLikeCount/{userId}")
    R<Integer> getLikeCount( @PathVariable("userId")Long userId);
    /**
     * 获取博客总数
     */
    @GetMapping("/blog/getBlogTotal")
    R<Integer> getBlogTotal();
    /**
     * 获取博客列表
     */
    @GetMapping("/blog/getBlogListByIds")
    R<List<BlogDto>> getBlogListByIds(List<Long> sourceIdList);
}
