package com.smartLive.blog.controller;

import com.smartLive.blog.domain.Blog;
import com.smartLive.blog.domain.VO.BlogVO;
import com.smartLive.blog.service.IBlogService;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.utils.poi.ExcelUtil;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.common.core.web.page.TableDataInfo;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 博客内部服务控制器
 * 提供给其他微服务（如互动模块、审核模块）调用的 Feign 内部接口，负责数据同步、状态变更及批量详情查询。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@RestController
@RequestMapping("/inner/blog")
public class BlogInnerController extends BaseController
{
    @Autowired
    private IBlogService blogService;
    /**
     * 更新博客状态
     */
    @PostMapping("/updateBlogStatus")
    Boolean updateBlogStatus(@RequestParam("targetId") Long targetId, @RequestParam("status") Integer status, @RequestParam(value = "reason", required = false) String reason){
        return blogService.updateBlogStatus(targetId, status, reason);
    }
    /**
     * 获取博客及其关联数据的详情对象（VO）
     * 
     * @param id 博客ID
     * @return 包含互动数据与博主信息的视图对象
     */
    @GetMapping("/getBlogById/{id}")
    BlogVO getBlogById(@PathVariable("id") Long id){
        return blogService.getBlogById(id);
    }
    /**
     * 获取指定用户的博文发布总数
     * 
     * @param userId 用户ID
     * @return 博文总数
     */
    @GetMapping("/getBlogCount/{userId}")
    Integer getBlogCount(@PathVariable("userId")Long userId){
        return blogService.getBlogCount(userId);
    }
    /**
     * 获取指定用户名下所有博文累计获赞总数
     * 
     * @param userId 用户ID
     * @return 获赞总数
     */
    @GetMapping("/getUserLikeCount/{userId}")
    Integer getLikeCount( @PathVariable("userId")Long userId){
        return blogService.getLikeCount(userId);
    }
    /**
     * 获取博客总数
     */
    @GetMapping("/getBlogTotal")
    Integer getBlogTotal() {
        return blogService.getBlogTotal();
    }
    /**
     * 获取博客列表
     */
    @GetMapping("/getBlogListByIds")
    List<BlogVO> getBlogListByIds(@RequestParam("sourceIdList") List<Long> sourceIdList){
        return blogService.getBlogListByIds(sourceIdList);
    }
    /**
     * 批量更新点赞数
     */
    @PostMapping("/updateLikeCountBatch")
    Boolean updateLikeCountBatch(@RequestBody  Map<Long, Integer> updateMap){
        return blogService.updateLikeCountBatch(updateMap);
    }
    /**
     * 批量更新评论数
     */
    @PostMapping("/updateCommentCountBatch")
    Boolean updateCommentCountBatch(@RequestBody Map<Long, Integer> updateMap){
        return blogService.updateCommentCountBatch(updateMap);
    }
    /**
     * 批量更新收藏数
     */
    @PostMapping("/updateStarCountBatch")
    Boolean updateStarCountBatch(@RequestBody Map<Long, Integer> updateMap){
        return blogService.updateStarCountBatch(updateMap);
    }
    /**
     * 获取博客点赞数
     * @param sourceId 博客ID
     * @return 博客点赞数
     */
    @GetMapping("/getBlogLikeCount/{sourceId}")
    Integer getBlogLikeCount(@PathVariable("sourceId") Long sourceId){
        return blogService.getBlogLikeCount(sourceId);
    }
    /**
     * 获取博客收藏数
     */
    @GetMapping("/getBlogStarCount/{sourceId}")
    Integer getStarCount(@PathVariable("sourceId") Long sourceId){
        return blogService.getBlogStarCount(sourceId);
    }

    /**
     * 获取全部博客ID列表（供热榜全量重建使用）
     */
    @GetMapping("/getAllBlogIds")
    public List<Long> getAllBlogIds() {
        return blogService.query().eq("status", 0)
                .ne("audit_status", 2)
                .ne("audit_status", 3)
                .select("id")
                .list()
                .stream()
                .map(blog -> blog.getId())
                .collect(java.util.stream.Collectors.toList());
    }
}
