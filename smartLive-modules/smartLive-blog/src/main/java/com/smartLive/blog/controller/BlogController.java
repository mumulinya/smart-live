package com.smartLive.blog.controller;

import com.smartLive.blog.domain.Blog;
import com.smartLive.blog.service.IBlogService;
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

/**
 * 博客Controller
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@RestController
@RequestMapping("/blog")
public class BlogController extends BaseController
{
    @Autowired
    private IBlogService blogService;

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

    @GetMapping("/blogList")
    public AjaxResult blogList(Blog blog)
    {
        List<Blog> list = blogService.selectBlogList(blog);
        return success(list);
    }

    /**
     * 刷新缓存
     */
    @GetMapping("/flushCache")
    public AjaxResult flushCache() {
        return success(blogService.flashCache());
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
     * 全量发布博客
     */
    @PostMapping("/allPublish")
    public AjaxResult allPublish() {
        return success(blogService.allPublish());
    }

    /**
     * 发布博客
     */
    @PostMapping("/publish/{ids}")
    public AjaxResult allPublish(@PathVariable String[] ids) {
        return success(blogService.publish(ids));
    }


    /**
     * 发布博文
     * @param blog
     * @return
     */
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog ) {
        return Result.ok(blogService.saveBlog(blog));
    }

    /**
     * 点赞博文
     * @param id
     * @return
     */
    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        Boolean b = blogService.likeBlog(id);
        if (b) {
            return Result.ok("操作成功");
        }
        return Result.fail("操作失败");
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

        return Result.ok(blogService.queryHotBlog(current));
    }
    @GetMapping("/category/{typeId}")
    public Result queryBlogByCategory(@PathVariable("typeId") Long typeId,@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return Result.ok(blogService.queryBlogByCategory(typeId,current));
    }
    /**
     * 查询博文详情
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id") Long id) {

        return Result.ok(blogService.queryBlogById(id));
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
        return Result.ok(blogService.queryBlogByUserId(current, userId));
    }
    /**
     * 查询关注用户发布的博文
     * @param max
     * @param offset
     * @return
     */
    @GetMapping("/of/follow")
    public Result queryBlogByFollow(@RequestParam(value = "lastId") Long max, @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        return Result.ok(blogService.queryBlogByFollow(max, offset));
    }
}
