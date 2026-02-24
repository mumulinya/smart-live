package com.smartLive.interaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.interaction.domain.Comment;
import com.smartLive.interaction.domain.VO.CommentVO;

import java.util.List;
import java.util.Map;

/**
 * 评论Service接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface ICommentService extends IService<Comment> {
    /**
     * 查询评论
     *
     * @param id 评论主键
     * @return 评论
     */
     Comment selectCommentById(Long id);

    /**
     * 查询评论列表
     *
     * @param comment 评论
     * @return 评论集合
     */
     List<Comment> selectCommentList(Comment comment);

    /**
     * 新增评论
     *
     * @param comment 评论
     * @return 结果
     */
     int insertComment(Comment comment);

    /**
     * 修改评论
     *
     * @param comment 评论
     * @return 结果
     */
     int updateComment(Comment comment);

    /**
     * 批量删除评论
     *
     * @param ids 需要删除的评论主键集合
     * @return 结果
     */
     int deleteCommentByIds(Long[] ids);

    /**
     * 删除评论信息
     *
     * @param id 评论主键
     * @return 结果
     */
     int deleteCommentById(Long id);

    /**
     * 获取评论列表
     *
     * @param comment
     * @return
     */
    List<Comment> listComment(Comment comment, Integer current);

    /**
     * 新增评论
     *
     * @param comment
     * @return
     */
    Integer addComment(Comment comment);


    /**
     * 删除评论
     *
     * @param comment
     * @return
     */
    Boolean deleteComment(Comment comment);

    /**
     * 获取用户的评论
     *
     * @param current
     * @return
     */
    List<Comment> getCommentOfUser(Comment comment,Integer current);

    /**
     * 获取评论列表
     *
     * @return
     */
    List<Comment> getCommentList();


    /**
     * 获取用户发表的评论数
     *
     * @param comment
     * @return
     */
    Integer getCommentCount(Comment comment);

    /**
     * 创建ai自动创建的评论
     *
     * @param
     * @return
     */
    void aiCreateComment();

    /**
     * 获取评论总数
     *
     * @return
     */
    Integer getCommentTotal();

    /**
     * 获取评论列表
     *
     * @param sourceIdList
     * @return
     */
    List<Comment> getCommentListByIds(List<Long> sourceIdList);

    /**
     * 批量更新点赞数
     *
     * @param updateMap
     * @return
     */
    Boolean updateLikeCountBatch(Map<Long, Integer> updateMap);
    /**
     * 批量更新评论数
     *
     * @param updateMap
     * @return
     *  */
    Boolean updateCommentCountBatch(Map<Long, Integer> updateMap);
    /**
     * 获取评论点赞数
     *
     * @param sourceId
     * @return
     */
    Integer getCommentLikeCount(Long sourceId);
    /**
     * 根据id获取评论详情
     *
     * @param id
     * @return
     */
    Comment getCommentById(Long id);
    /**
     * 获取子评论列表
     *
     * @param comment
     * @param current
     * @return
     */
    List<Comment> listChildComment(Comment comment, Integer current);

    /**
     * 更新评论状态
     * @param id 评论ID
     * @param status 状态
     * @return
     */
    Boolean updateCommentStatus(Long id, Integer status);

    /**
     * 判断当前用户是否评论过目标资源
     *
     * @param comment 评论查询条件（至少包含 sourceType/sourceId）
     * @return 是否评论过
     */
    Boolean isComment(Comment comment);
}
