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

@RestController
@RequestMapping("/blog")
public class BlogController extends BaseController
{
    @Autowired
    private IBlogService blogService;
    /**
     * 分页查询博客列表 (后台管理端)
     */

    @RequiresPermissions("business:blog:list")
    @GetMapping("/list")
    public TableDataInfo list(Blog blog)
    {
        startPage();
        List<BlogVO> list = blogService.selectBlogVoList(blog);
        return getDataTable(list);
    }
    /**
     * 导出博客列表
     */

    @RequiresPermissions("business:blog:export")
    @Log(title = "blog", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Blog blog)
    {
        List<Blog> list = blogService.selectBlogList(blog);
        ExcelUtil<Blog> util = new ExcelUtil<Blog>(Blog.class);
        util.exportExcel(response, list, "blog list");
    }
    /**
     * 查询博客列表数据 (无分页)
     */

    @GetMapping("/blogList")
    public AjaxResult blogList(Blog blog)
    {
        List<BlogVO> list = blogService.selectBlogVoList(blog);
        return success(list);
    }
    /**
     * 清除并刷新博客缓存
     */

    @GetMapping("/flushCache")
    public AjaxResult flushCache() {
        return success(blogService.flashCache());
    }
    /**
     * 根据ID获取博客详细信息 (后台管理端)
     */

    @RequiresPermissions("business:blog:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(blogService.selectBlogVoById(id));
    }
    /**
     * 批量删除博客
     */

    @RequiresPermissions("business:blog:remove")
    @Log(title = "blog", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(blogService.deleteBlogByIds(ids));
    }
    /**
     * 删除单个博客
     */

    @Log(title = "blog", businessType = BusinessType.DELETE)
    @DeleteMapping("remove/{id}")
    public Result removeById(@PathVariable("id") Long id)
    {
        return Result.ok(blogService.deleteBlogById(id));
    }
    /**
     * 发布新博客
     */

    @PostMapping
    public Result saveBlog(@RequestBody Blog blog ) {
        return Result.ok(blogService.saveBlog(blog));
    }

    @GetMapping("/of/me")
    public Result queryMyBlog(Blog blog,@RequestParam(value = "current", defaultValue = "1") Integer current) {
        List<BlogVO> blogList=blogService.queryMyBlog(blog,current);
        return Result.ok(blogList);
    }

    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {

        return Result.ok(blogService.queryHotBlog(current));
    }
    @GetMapping("/category/{typeId}")
    public Result queryBlogByCategory(@PathVariable("typeId") Long typeId,@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return Result.ok(blogService.queryBlogByCategory(typeId,current));
    }
    /**
     * 查询指定用户的博客流
     */
    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("userId") Long userId) {
        return Result.ok(blogService.queryBlogByUserId(current, userId));
    }
    /**
     * 根据ID获取博客详细信息 (移动端)
     */
    @GetMapping("/getBlogById/{id}")
    public Result getBlogById(@PathVariable("id") Long id) {

        return Result.ok(blogService.queryBlogById(id));
    }
    /**
     * 设置博客是否置顶
     */
    @PutMapping("/isPin")
    public Result isPin(@RequestBody Blog blog){
        boolean pin = blogService.isPin(blog);
        if (pin){
            return Result.ok("pin updated");
        }else
            return Result.fail("pin update failed");
    }
    /**
     * 全量同步所有博客到搜索引擎 (全量导入)
     */
    @PostMapping("/allPublish")
    public AjaxResult allPublish() {
        return success(blogService.allPublish());
    }
    /**
     * 全量同步所有博客到搜索引擎 (全量导入)
     */

    @PostMapping("/publish/{ids}")
    public AjaxResult allPublish(@PathVariable String[] ids) {
        return success(blogService.publish(ids));
    }

    @GetMapping("/search")
    public Result searchBlogs(@RequestParam(name = "keyword", required = false) String keyword) {
        return Result.ok(blogService.searchBlogs(keyword));
    }
}
