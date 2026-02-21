package com.smartLive.interaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.interaction.domain.Review;

import java.util.List;
import java.util.Map;

/**
 * 评论Service接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface IReviewService extends IService<Review> {
    /**
     * 查询评论
     *
     * @param id 评论主键
     * @return 评论
     */
     Review selectReviewById(Long id);

    /**
     * 查询评论列表
     *
     * @param review 评论
     * @return 评论集合
     */
     List<Review> selectReviewList(Review review);

    /**
     * 新增评论
     *
     * @param review 评论
     * @return 结果
     */
     int insertReview(Review review);

    /**
     * 修改评论
     *
     * @param review 评论
     * @return 结果
     */
     int updateReview(Review review);

    /**
     * 批量删除评论
     *
     * @param ids 需要删除的评论主键集合
     * @return 结果
     */
     int deleteReviewByIds(Long[] ids);

    /**
     * 删除评论信息
     *
     * @param id 评论主键
     * @return 结果
     */
     int deleteReviewById(Long id);

    /**
     * 获取评论列表
     *
     * @param review
     * @return
     */
    List<Review> listReview(Review review, Integer current);

    /**
     * 新增评论
     *
     * @param review
     * @return
     */
    Integer addReview(Review review);


    /**
     * 删除评论
     *
     * @param review
     * @return
     */
    Boolean deleteReview(Review review);

    /**
     * 获取用户的评论
     *
     * @param current
     * @return
     */
    List<Review> getReviewOfUser(Review review,Integer current);

    /**
     * 获取用户发表的评论数
     *
     * @param review
     * @return
     */
    Integer getReviewCount(Review review);

    /**
     * 获取评论总数
     *
     * @return
     */
    Integer getReviewTotal();

    /**
     * 获取评论列表
     *
     * @param sourceIdList
     * @return
     */
    List<Review> getReviewListByIds(List<Long> sourceIdList);

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
     * 批量更新收藏数
     *
     * @param updateMap
     * @return
     */
    Boolean updateStarCountBatch(Map<Long, Integer> updateMap);
    /**
     * 获取评论点赞数
     *
     * @param sourceId
     * @return
     */
    Integer getReviewLikeCount(Long sourceId);
    /**
     * 根据id获取评论详情
     *
     * @param id
     * @return
     */
    Review getReviewById(Long id);
    /**
     * 获取评价收藏数
     *
     * @param sourceId
     * @return
     */
    Integer getReviewStarCount(Long sourceId);

    /**
     * 更新评价状态
     * @param id 评价ID
     * @param status 状态
     * @return
     */
    Boolean updateReviewStatus(Long id, Integer status);

    /**
     * 判断当前用户是否评价过目标资源
     *
     * @param review 评价查询条件（至少包含 sourceType/sourceId）
     * @return 是否评价过
     */
    Boolean isReview(Review review);
}
