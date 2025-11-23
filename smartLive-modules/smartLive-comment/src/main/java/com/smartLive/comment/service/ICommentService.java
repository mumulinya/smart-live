package com.smartLive.comment.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.comment.domain.Comment;
import com.smartLive.comment.domain.CommentDTO;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.web.domain.Result;

/**
 * 评论Service接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface ICommentService  extends IService<Comment>
{
    /**
     * 查询评论
     * 
     * @param id 评论主键
     * @return 评论
     */
    public Comment selectCommentById(Long id);

    /**
     * 查询评论列表
     * 
     * @param comment 评论
     * @return 评论集合
     */
    public List<Comment> selectCommentList(Comment comment);

    /**
     * 新增评论
     * 
     * @param comment 评论
     * @return 结果
     */
    public int insertComment(Comment comment);

    /**
     * 修改评论
     * 
     * @param comment 评论
     * @return 结果
     */
    public int updateComment(Comment comment);

    /**
     * 批量删除评论
     * 
     * @param ids 需要删除的评论主键集合
     * @return 结果
     */
    public int deleteCommentByIds(Long[] ids);

    /**
     * 删除评论信息
     * 
     * @param id 评论主键
     * @return 结果
     */
    public int deleteCommentById(Long id);

    /**
     * 获取评论列表
     * @param comment
     * @return
     */
    Result listComment(Comment comment,Integer current);

    /**
     * 新增评论
     * @param comment
     * @return
     */
    Result addComment(Comment comment);

    /**
     * 获取我的评论
     * @param current
     * @return
     */
    Result getCommentOfMe(Integer current);

    /**
     * 获取评论列表
     * @return
     */
    List<Comment> getCommentList();

    /**
     * 保存ai自动创建的评论
     * @param comments
     * @return
     */
    Result saveAiCreateComment(List<CommentDTO> comments);
    /**
     * 获取用户发表的评论数
     * @param userId
     * @return
     */
    Integer getCommentCount(Long userId);

    /**
     * 创建ai自动创建的评论
     * @param
     * @return
     */
    void aiCreateComment( );
    /**
     * 获取评论总数
     * @return
     */
    Integer getCommentTotal();
}
