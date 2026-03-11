package com.smartLive.blog.controller;

import com.smartLive.blog.domain.Blog;
import com.smartLive.blog.domain.VO.BlogVO;
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
 * 博客管理控制器
 * 提供了博客的发布、修改、后台管理列表以及移动端热门/分类/个人博文流的查询接口。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@RestController
@RequestMapping("/blog")
public class BlogController extends BaseController
{
    @Autowired
    private IBlogService blogService;

    /**
     * 查询博客列表（带权限控制）
     * 权限: business:blog:list
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
     * 导出博客列表（带权限控制）
     * 权限: business:blog:export
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
     * 获取博客列表
     */
    @GetMapping("/blogList")
    public AjaxResult blogList(Blog blog)
    {
        List<Blog> list = blogService.selectBlogList(blog);
        return success(list);
    }

    /**
     * 刷新博客详情缓存与排行榜。
     * 适用于数据大面积变动或缓存异常时的手动重置。
     *
     * @return 刷新成功标识
     */
    @GetMapping("/flushCache")
    public AjaxResult flushCache() {
        return success(blogService.flashCache());
    }

    /**
     * 获取博客详细信息（带权限控制）
     * 权限: business:blog:query
     */
    @RequiresPermissions("business:blog:query")
//    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(blogService.selectBlogById(id));
    }

    /**
     * 修改博客
     */
    @Log(title = "博客", businessType = BusinessType.UPDATE)
    @PutMapping
    public Result edit(@RequestBody Blog blog)
    {
        return Result.ok(blogService.updateBlog(blog));
    }

    /**
     * 删除博客列表（带权限控制）
     * 权限: business:blog:remove
     */
    @RequiresPermissions("business:blog:remove")
    @Log(title = "博客", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(blogService.deleteBlogByIds(ids));
    }

    /**
     * 删除单个博客
     */
    @Log(title = "博客", businessType = BusinessType.DELETE)
    @DeleteMapping("remove/{id}")
    public Result removeById(@PathVariable("id") Long id)
    {
        return Result.ok(blogService.deleteBlogById(id));
    }

    /**
     * 新增博文
     * @param blog 博客实体
     * @return 操作结果
     */
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog ) {
        return Result.ok(blogService.saveBlog(blog));
    }

    /**
     * 查询我的博文
     * @param blog 博客查询条件
     * @param current 当前页码
     * @return 我的博文列表
     */
    @GetMapping("/of/me")
    public Result queryMyBlog(Blog blog,@RequestParam(value = "current", defaultValue = "1") Integer current) {
        List<BlogVO> blogList=blogService.queryMyBlog(blog,current);
        return Result.ok(blogList);
    }

    /**
     * 查询热门博文
     * @param current 当前页码
     * @return 热门博文列表
     */
    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {

        return Result.ok(blogService.queryHotBlog(current));
    }
    /**
     * 根据分类查询博客
     * @param typeId 分类ID
     * @param current 当前页码
     * @return 分类下的博客列表
     */
    @GetMapping("/category/{typeId}")
    public Result queryBlogByCategory(@PathVariable("typeId") Long typeId,@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return Result.ok(blogService.queryBlogByCategory(typeId,current));
    }
    /**
     * 查询用户发布的博文
     * @param current 当前页码
     * @param userId 用户ID
     * @return 用户发布的博客列表
     */
    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("userId") Long userId) {
        return Result.ok(blogService.queryBlogByUserId(current, userId));
    }
    /**
     * 查询博文详情
     * @param id 博客ID
     * @return 博客详情
     */
    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id") Long id) {

        return Result.ok(blogService.queryBlogById(id));
    }
    /**
     * 设置/取消博客置顶
     * 
     * @param blog 博客实体（需包含 id 和 isPin 状态）
     * @return 操作成功信息或失败提示
     */
    @PutMapping("/isPin")
    public Result isPin(@RequestBody Blog blog){
        boolean pin = blogService.isPin(blog);
        if (pin){
            return Result.ok("操作成功");
        }else
            return Result.fail("操作失败");
    }
    /**
     * 全量发布/同步博客数据至搜索库与向量库
     *
     * @return 触发结果
     */
    @PostMapping("/allPublish")
    public AjaxResult allPublish() {
        return success(blogService.allPublish());
    }

    /**
     * 批量发布/同步指定 ID 的博客
     *
     * @param ids 博客 ID 数组
     * @return 触发结果
     */
    @PostMapping("/publish/{ids}")
    public AjaxResult allPublish(@PathVariable String[] ids) {
        return success(blogService.publish(ids));
    }
}
