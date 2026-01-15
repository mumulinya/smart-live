package com.smartLive.blog.controller;

import com.smartLive.blog.domain.Blog;
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
 * 博客Controller
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@RestController
@RequestMapping("/inner/blog")
public class BlogInnerController extends BaseController
{
    @Autowired
    private IBlogService blogService;
    /**
     * 获取博客详情
     * @param id
     * @return
     */
    @GetMapping("/getBlogById/{id}")
    Blog getBlogById( @PathVariable("id")Long id){
        return blogService.getBlogById(id);
    }
    /**
     * 获取博客数量
     * @param userId
     * @return
     */
    @GetMapping("/getBlogCount/{userId}")
    Integer getBlogCount(@PathVariable("userId")Long userId){
        return blogService.getBlogCount(userId);
    }
    /**
     * 获取博客点赞数
     * @param userId
     * @return
     */
    @GetMapping("/getLikeCount/{userId}")
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
    List<Blog> getBlogListByIds(@RequestParam("sourceIdList") List<Long> sourceIdList){
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
}
