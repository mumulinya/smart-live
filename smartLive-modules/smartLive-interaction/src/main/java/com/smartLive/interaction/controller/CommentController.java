package com.smartLive.interaction.controller;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.utils.poi.ExcelUtil;
import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.AjaxResult;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.common.core.web.page.TableDataInfo;
import com.smartLive.common.log.annotation.Log;
import com.smartLive.common.log.enums.BusinessType;
import com.smartLive.common.security.annotation.RequiresPermissions;
import com.smartLive.interaction.domain.Comment;
import com.smartLive.interaction.service.ICommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 评论管理外部接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@RestController
@RequestMapping("/comment")
public class CommentController extends BaseController
{
    @Autowired
    private ICommentService commentService;

    /**
     * 查询评论列表（带权限控制）
     */
    @RequiresPermissions("comment:comment:list")
    @GetMapping("/list")
    public TableDataInfo list(Comment comment)
    {
        startPage();
        List<Comment> list = commentService.selectCommentList(comment);
        return getDataTable(list);
    }

    /**
     * 导出评论列表（带权限控制）
     */
    @RequiresPermissions("comment:comment:export")
    @Log(title = "评论", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Comment comment)
    {
        List<Comment> list = commentService.selectCommentList(comment);
        ExcelUtil<Comment> util = new ExcelUtil<Comment>(Comment.class);
        util.exportExcel(response, list, "评论数据");
    }

    /**
     * 获取评论详细信息（带权限控制）
     */
    @RequiresPermissions("comment:comment:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(commentService.selectCommentById(id));
    }

    /**
     * 新增评论（带权限控制）
     */
    @RequiresPermissions("comment:comment:add")
    @Log(title = "评论", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Comment comment)
    {
        comment.setCreateTime(DateUtils.getNowDate());
        comment.setUpdateTime(DateUtils.getNowDate());
        return toAjax(commentService.insertComment(comment));
    }

    /**
     * 修改评论（带权限控制）
     */
    @RequiresPermissions("comment:comment:edit")
    @Log(title = "评论", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Comment comment)
    {
        return toAjax(commentService.updateComment(comment));
    }

    /**
     * 删除评论（带权限控制）
     */
    @RequiresPermissions("comment:comment:remove")
    @Log(title = "评论", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(commentService.deleteCommentByIds(ids));
    }

    /**
     * 获取评论列表（分页）
     */
    @GetMapping("/listComment")
    public Result listComment(Comment  comment,@RequestParam("current") Integer current){
        return Result.ok(commentService.listComment(comment,current));
    }
    /**
     * 获取子评论列表（分页）
     */
    @GetMapping("/listChildComment")
    public Result listChildComment(Comment comment,@RequestParam("current") Integer current){
        return Result.ok(commentService.listChildComment(comment,current));
    }
    /**
     * 添加评论
     */
    @PostMapping("/addComment")
    public Result addComment(@RequestBody Comment comment)
    {
        Integer i = commentService.addComment(comment);
        if (i > 0) {
            return Result.ok("添加成功");
        }
        return Result.fail("添加失败");
    }
    /**
     * 删除评论
     */
    @PostMapping("/removeComment")
    public Result removeComment(@RequestBody Comment comment)
    {

        return Result.ok(commentService.deleteComment(comment));
    }
    /**
     * 获取用户的评论列表
     */
    @GetMapping("/of/user")
    public Result getCommentOfUser(Comment comment,@RequestParam("current") Integer current){
        return Result.ok(commentService.getCommentOfUser(comment,current));
    }
    /**
     * 根据ID获取评论详情
     */
    @GetMapping("/getComment/{id}")
    public Result getCommentById(@PathVariable("id")Long id){
        return Result.ok(commentService.getCommentById(id));
    }
    /**
     * AI创建评论
     */
    @PostMapping("/aiCreateComment")
    public Result aiCreateComment(){
         commentService.aiCreateComment();
        return Result.ok("创建成功");
    }
    /**
     * 获取所有评论列表
     * @return 评论列表
     */
    @GetMapping("/list2")
    List<Comment> getCommentList(){
        return commentService.getCommentList();
    }

    /**
     * 保存AI创建的评论到Redis
     * @param comments 评论列表
     * @return 操作结果
     */
    @PostMapping("/saveAiCreateComment")
    public Result saveAiCreateComment(@RequestBody List<Comment> comments){
        return Result.ok(commentService.saveAiCreateComment(comments));
    }

    /**
     * 获取评论数量
     * @param comment 评论查询条件
     * @return 评论数量
     */
    @GetMapping("/getCommentCount")
    R<Integer> getCommentCount( Comment comment){
        return R.ok(commentService.getCommentCount(comment));
    }
    /**
     * 获取评论总数
     * @return 评论总数
     */
    @GetMapping("/getCommentTotal")
    R<Integer> getCommentTotal(){
        return R.ok(commentService.getCommentTotal());
    }
}
