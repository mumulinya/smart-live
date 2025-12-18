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
import com.smartLive.interaction.domain.CommentDTO;
import com.smartLive.interaction.service.ICommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 评论Controller
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
     * 查询评论列表
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
     * 导出评论列表
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
     * 获取评论详细信息
     */
    @RequiresPermissions("comment:comment:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(commentService.selectCommentById(id));
    }

    /**
     * 新增评论
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
     * 修改评论
     */
    @RequiresPermissions("comment:comment:edit")
    @Log(title = "评论", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Comment comment)
    {
        return toAjax(commentService.updateComment(comment));
    }

    /**
     * 删除评论
     */
    @RequiresPermissions("comment:comment:remove")
    @Log(title = "评论", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(commentService.deleteCommentByIds(ids));
    }


    @GetMapping("/listComment")
    public Result listComment(Comment  comment,Integer current){
        System.out.println("current:"+current);
        return commentService.listComment(comment,current);
    }

    @PostMapping("/addComment")
    public Result addComment(@RequestBody Comment comment)
    {

        return commentService.addComment(comment);
    }
    @GetMapping("/of/me")
    public Result getCommentOfMe(Integer current){
        return commentService.getCommentOfMe(current);
    }

    @PostMapping("/aiCreateComment")
    public Result aiCreateComment(){
         commentService.aiCreateComment();
        return Result.ok("创建成功");
    }
    @GetMapping("/comment/list")
    List<Comment> getCommentList(){
        return commentService.getCommentList();
    }

    /**
     * 保存ai创建的评论存入redis
     * @param comments
     * @return
     */
    @PostMapping("/comment/saveAiCreateComment")
    public Result saveAiCreateComment(@RequestBody List<CommentDTO> comments){
        return commentService.saveAiCreateComment(comments);
    }

    /**
     * 获取评论数量
     * @param userId
     * @return
     */
    @GetMapping("/comment/getCommentCount/{userId}")
    R<Integer> getCommentCount( @PathVariable("userId")Long userId){
        return R.ok(commentService.getCommentCount(userId));
    }
    /**
     * 获取评论总数
     * @return
     */
    @GetMapping("/comment/getCommentTotal")
    R<Integer> getCommentTotal(){
        return R.ok(commentService.getCommentTotal());
    }
}
