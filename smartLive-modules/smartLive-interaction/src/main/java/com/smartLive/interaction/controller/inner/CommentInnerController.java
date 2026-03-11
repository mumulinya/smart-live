package com.smartLive.interaction.controller.inner;

import com.smartLive.common.core.web.controller.BaseController;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.domain.Comment;
import com.smartLive.interaction.domain.VO.CommentVO;
import com.smartLive.interaction.service.ICommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论管理内部控制层
 * 提供 AI 评论触发、全量数据导出及跨模块状态更新等 RPC 支持。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@RestController
@RequestMapping("/inner/comment")
public class CommentInnerController extends BaseController
{
    @Autowired
    private ICommentService commentService;

    /**
     * AI创建评论（内部接口）
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
    @GetMapping("/list")
    List<CommentVO> getCommentList(){
        return commentService.getCommentList();
    }

    /**
     * 获取评论数量
     * @param comment 评论查询条件
     * @return 评论数量
     */
    @GetMapping("/getCommentCount")
    Integer getCommentCount( Comment comment){
        return commentService.getCommentCount(comment);
    }
    /**
     * 获取评论总数
     * @return 评论总数
     */
    @GetMapping("/getCommentTotal")
    Integer getCommentTotal(){
        return commentService.getCommentTotal();
    }

    /**
     * 判断当前用户是否评论过目标资源
     */
    @GetMapping("/isComment")
    public Boolean isComment(Comment comment){
        return commentService.isComment(comment);
    }
    /**
     * 更新评论状态
     * @param targetId
     * @param status
     * @return
     */
    @PostMapping("/updateCommentStatus")
    Boolean updateCommentStatus(@RequestParam("targetId") Long targetId, @RequestParam("status") Integer status){
        return commentService.updateCommentStatus(targetId, status);
    }
}
