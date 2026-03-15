package com.smartLive.interaction.controller.api;
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
import com.smartLive.interaction.domain.VO.CommentVO;
import com.smartLive.interaction.service.ICommentService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 评论管理控制层
 * 提供全站内容的评论发布、层级查询（一级/二级子评论）、删除及点赞互动等功能。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@RestController
@RequestMapping("/comment")
public class CommentController extends BaseController
{
    @Autowired
    private ICommentService commentService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 查询评论列表（带权限控制）
     */
    @RequiresPermissions("comment:comment:list")
    @GetMapping("/list")
    public TableDataInfo list(Comment comment)
    {
        startPage();
        List<CommentVO> list = commentService.selectCommentList(comment);
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
        List<CommentVO> list = commentService.selectCommentList(comment);
        ExcelUtil<CommentVO> util = new ExcelUtil<CommentVO>(CommentVO.class);
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
     * 分页查询一级评论列表
     * 支持按照时间或热度（点赞/回复数）进行动态排序显示。
     *
     * @param comment 过滤条件 (如来源 ID、类型)
     * @param current 页码
     * @param sort    排序字段 (default/new)
     * @return 一级评论列表
     */
    @GetMapping("/listComment")
    public Result listComment(Comment  comment,@RequestParam("current") Integer current,
                              @RequestParam(value = "sort", required = false) String sort){
        return Result.ok(commentService.listComment(comment,current,sort));
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
     * 校验当前用户是否已发布过针对该资源的评论
     * 常用于防重复发布或特定的互动状态显示。
     *
     * @param comment 评论检索条件
     * @return 布尔结果
     */
    @GetMapping("/isComment")
    public Result isComment(Comment comment){
        return Result.ok(commentService.isComment(comment));
    }

}
