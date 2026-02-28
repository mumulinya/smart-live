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
     * 获取评论列表（分页）
     *
     * @param comment 评论查询条件
     * @param current 当前页码
     * @return 评论列表
     */
    List<Comment> listComment(Comment comment, Integer current);

    /**
     * 用户新增评论
     *
     * @param comment 评论实体
     * @return 评论ID
     */
    Integer addComment(Comment comment);


    /**
     * 用户删除评论
     *
     * @param comment 评论实体（包含ID和用户信息）
     * @return 删除结果
     */
    Boolean deleteComment(Comment comment);

    /**
     * 获取用户的评论列表
     *
     * @param comment 评论查询条件
     * @param current 当前页码
     * @return 用户评论列表
     */
    List<Comment> getCommentOfUser(Comment comment, Integer current);

    /**
     * 获取全部评论列表
     *
     * @return 评论列表
     */
    List<Comment> getCommentList();


    /**
     * 获取用户发表的评论数
     *
     * @param comment 评论查询条件
     * @return 评论数量
     */
    Integer getCommentCount(Comment comment);

    /**
     * AI自动创建评论
     */
    void aiCreateComment();

    /**
     * 获取评论总数
     *
     * @return 评论总数
     */
    Integer getCommentTotal();

    /**
     * 根据ID列表批量获取评论
     *
     * @param sourceIdList 评论ID列表
     * @return 评论列表
     */
    List<Comment> getCommentListByIds(List<Long> sourceIdList);

    /**
     * 批量更新评论点赞数
     *
     * @param updateMap 评论ID与点赞数的映射
     * @return 更新结果
     */
    Boolean updateLikeCountBatch(Map<Long, Integer> updateMap);
    /**
     * 批量更新子评论数
     *
     * @param updateMap 评论ID与子评论数的映射
     * @return 更新结果
     */
    Boolean updateCommentCountBatch(Map<Long, Integer> updateMap);
    /**
     * 获取评论点赞数
     *
     * @param sourceId 评论ID
     * @return 点赞数量
     */
    Integer getCommentLikeCount(Long sourceId);
    /**
     * 根据ID获取评论详情
     *
     * @param id 评论主键
     * @return 评论实体
     */
    Comment getCommentById(Long id);
    /**
     * 获取子评论列表（分页）
     *
     * @param comment 父评论查询条件
     * @param current 当前页码
     * @return 子评论列表
     */
    List<Comment> listChildComment(Comment comment, Integer current);

    /**
     * 更新评论状态（审核通过/拒绝）
     *
     * @param id     评论ID
     * @param status 状态
     * @return 更新结果
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
