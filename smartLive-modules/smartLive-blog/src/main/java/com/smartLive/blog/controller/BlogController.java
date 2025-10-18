package com.smartLive.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartLive.blog.domain.Blog;
import com.smartLive.blog.service.IBlogService;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.domain.UserDTO;
import com.smartLive.common.core.utils.poi.ExcelUtil;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.common.core.web.page.TableDataInfo;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 博客Controller
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@RestController
//@RequestMapping("/blog")
public class BlogController extends BaseController
{
    @Autowired
    private IBlogService blogService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查询博客列表
     */
    @RequiresPermissions("business:blog:list")
    @GetMapping("/list")
    public TableDataInfo list(Blog blog)
    {
        startPage();
        List<Blog> list = blogService.selectBlogList(blog);
        return getDataTable(list);
    }

    /**
     * 导出博客列表
     */
    @RequiresPermissions("business:blog:export")
    @Log(title = "博客", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Blog blog)
    {
        List<Blog> list = blogService.selectBlogList(blog);
        ExcelUtil<Blog> util = new ExcelUtil<Blog>(Blog.class);
        util.exportExcel(response, list, "博客数据");
    }

    /**
     * 刷新缓存
     */
    @GetMapping("/flushCache")
    public AjaxResult flushCache() {
        return success(blogService.flushCache());
    }

    /**
     * 获取博客详细信息
     */
    @RequiresPermissions("business:blog:query")
//    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(blogService.selectBlogById(id));
    }

//    /**
//     * 新增博客
//     */
//    @RequiresPermissions("business:blog:add")
//    @Log(title = "博客", businessType = BusinessType.INSERT)
//    @PostMapping
//    public AjaxResult add(@RequestBody Blog blog)
//    {
//        return toAjax(blogService.insertBlog(blog));
//    }

    /**
     * 修改博客
     */
    @RequiresPermissions("business:blog:edit")
    @Log(title = "博客", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Blog blog)
    {
        return toAjax(blogService.updateBlog(blog));
    }

    /**
     * 删除博客
     */
    @RequiresPermissions("business:blog:remove")
    @Log(title = "博客", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(blogService.deleteBlogByIds(ids));
    }


    /**
     * 发布博文
     * @param blog
     * @return
     */
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog ) {
        return blogService.saveBlog(blog);
    }

    /**
     * 点赞博文
     * @param id
     * @return
     */
    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        return blogService.likeBlog(id);
    }

    /**
     * 查询我的博文
     * @param current
     * @return
     */
    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        List<Blog> blogList=blogService.queryMyBlog(current);
        return Result.ok(blogList);
    }

    /**
     * 查询热门博文
     * @param current
     * @return
     */
    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {

        return blogService.queryHotBlog(current);
    }
    /**
     * 查询博文详情
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id") Long id) {

        return blogService.queryBlogById(id);
    }

    /**
     * 查询博文点赞数
     * @param id
     * @return
     */
    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable("id") Long id) {
        return blogService.queryBlogLikes(id);
    }

    /**
     * 查询用户发布的博文
     * @param current
     * @param userId
     * @return
     */
    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("userId") Long userId) {
        return blogService.queryBlogByUserId(current, userId);
    }
    /**
     * 查询关注用户发布的博文
     * @param max
     * @param offset
     * @return
     */
    @GetMapping("/of/follow")
    public Result queryBlogByFollow(@RequestParam(value = "lastId") Long max, @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        return blogService.queryBlogByFollow(max, offset);
    }

    @PostMapping("/blog/updateCommentById/{id}")
    public R<Boolean> updateCommentById(@PathVariable("id") Long blogId){
        return blogService.updateCommentById(blogId);
    }

    @GetMapping("/blog/getBlogById/{id}")
    R<Blog> getBlogById( @PathVariable("id")Long id){
        return blogService.getBlogById(id);
    }
    @GetMapping("/blog/getBlogCount/{userId}")
    R<Integer> getBlogCount(@PathVariable("userId")Long userId){
        Integer count = blogService.getBlogCount(userId);
        return R.ok(count);
    }
    /**
     * 获取博客点赞数
     * @param userId
     * @return
     */
    @GetMapping("/blog/getLikeCount/{userId}")
    R<Integer> getLikeCount( @PathVariable("userId")Long userId){
        return R.ok(blogService.getLikeCount(userId));

    }
}
