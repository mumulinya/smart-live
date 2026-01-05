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
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论Controller
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@RestController
@RequestMapping("/inner/comment")
public class CommentInnerController extends BaseController
{
    @Autowired
    private ICommentService commentService;

    @PostMapping("/aiCreateComment")
    public Result aiCreateComment(){
         commentService.aiCreateComment();
        return Result.ok("创建成功");
    }
    /**
     * 获取所有评论列表
     * @return
     */
    @GetMapping("/list")
    List<Comment> getCommentList(){
        return commentService.getCommentList();
    }

    /**
     * 获取评论数量
     * @param
     * @return
     */
    @GetMapping("/getCommentCount")
    Integer getCommentCount( Comment comment){
        return commentService.getCommentCount(comment);
    }
    /**
     * 获取评论总数
     * @return
     */
    @GetMapping("/getCommentTotal")
    Integer getCommentTotal(){
        return commentService.getCommentTotal();
    }
}
