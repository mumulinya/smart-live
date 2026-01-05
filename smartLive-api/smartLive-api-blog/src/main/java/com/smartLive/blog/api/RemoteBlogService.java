package com.smartLive.blog.api;

import com.smartLive.blog.api.dto.BlogDto;
import com.smartLive.blog.api.factory.RemoteBlogFallbackFactory;
import com.smartLive.common.core.constant.ServiceNameConstants;

import com.smartLive.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;


@FeignClient(contextId = "remoteBlogService", value = ServiceNameConstants.BLOG_SERVICE, fallbackFactory = RemoteBlogFallbackFactory.class)
public interface RemoteBlogService {

    /**
     * 获取博客详情
     *
     * @param id
     * @return
     */
    @GetMapping("/inner/blog/getBlogById/{id}")
    BlogDto getBlogById(@PathVariable("id") Long id);

    /**
     * 获取博客数
     *
     * @param userId
     * @return
     */
    @GetMapping("/blog/getBlogCount/{userId}")
    Integer getBlogCount(@PathVariable("userId") Long userId);

    /**
     * 获取博客点赞数
     *
     * @param userId
     * @return
     */
    @GetMapping("/inner/blog/getLikeCount/{userId}")
    Integer getLikeCount(@PathVariable("userId") Long userId);

    /**
     * 获取博客总数
     */
    @GetMapping("/inner/blog/getBlogTotal")
    Integer getBlogTotal();

    /**
     * 获取博客列表
     */
    @GetMapping("/inner/blog/getBlogListByIds")
    List<BlogDto> getBlogListByIds(@SpringQueryMap List<Long> sourceIdList);

    /**
     * 批量更新点赞数
     */
    @PostMapping("/inner/blog/updateLikeCountBatch")
    Boolean updateLikeCountBatch(@RequestBody  Map<Long, Integer> updateMap);

    /**
     * 批量更新评论数
     */
    @PostMapping("/inner/blog/updateCommentCountBatch")
    Boolean updateCommentCountBatch(@RequestBody Map<Long, Integer> updateMap);
}
