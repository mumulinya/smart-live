package com.smartLive.blog.api;

import com.smartLive.blog.api.DTO.BlogDTO;
import com.smartLive.blog.api.factory.RemoteBlogFallbackFactory;
import com.smartLive.common.core.constant.ServiceNameConstants;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

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
    BlogDTO getBlogById(@PathVariable("id") Long id);

    /**
     * 获取博客数
     *
     * @param userId
     * @return
     */
    @GetMapping("/inner/blog/getBlogCount/{userId}")
    Integer getBlogCount(@PathVariable("userId") Long userId);

    /**
     * 获取用户点赞数
     *
     * @param userId
     * @return
     */
    @GetMapping("/inner/blog/getUserLikeCount/{userId}")
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
    List<BlogDTO> getBlogListByIds(@RequestParam("sourceIdList")  List<Long> sourceIdList);

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
    /**
     * 批量更新收藏数
     */
    @PostMapping("/inner/blog/updateStarCountBatch")
    Boolean updateStarCountBatch(@RequestBody Map<Long, Integer> updateMap);
    /**
     * 获取博客点赞数
     */
    @GetMapping("/inner/blog/getBlogLikeCount/{sourceId}")
    Integer getBlogLikeCount(@PathVariable("sourceId") Long sourceId);
    /**
     * 获取博客收藏数
     */
    @GetMapping("/inner/blog/getBlogStarCount/{sourceId}")
    Integer getStarCount(@PathVariable("sourceId") Long sourceId);
    /**
     * 更新博客状态
     */
    @PostMapping("/inner/blog/updateBlogStatus")
    Boolean updateBlogStatus(@RequestParam("targetId") Long targetId,@RequestParam("status") Integer status);

    /**
     * 获取全部博客ID列表
     */
    @GetMapping("/inner/blog/getAllBlogIds")
    List<Long> getAllBlogIds();
}
