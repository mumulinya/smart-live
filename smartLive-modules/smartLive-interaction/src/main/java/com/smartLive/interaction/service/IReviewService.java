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
     * 获取评价列表（分页）
     *
     * @param review  评价查询条件
     * @param current 当前页码
     * @return 评价列表
     */
    List<Review> listReview(Review review, Integer current);

    /**
     * 用户新增评价
     *
     * @param review 评价实体
     * @return 评价ID
     */
    Integer addReview(Review review);


    /**
     * 用户删除评价
     *
     * @param review 评价实体（包含ID和用户信息）
     * @return 删除结果
     */
    Boolean deleteReview(Review review);

    /**
     * 获取用户的评价列表
     *
     * @param review  评价查询条件
     * @param current 当前页码
     * @return 用户评价列表
     */
    List<Review> getReviewOfUser(Review review, Integer current);

    /**
     * 获取用户发表的评价数
     *
     * @param review 评价查询条件
     * @return 评价数量
     */
    Integer getReviewCount(Review review);

    /**
     * 获取评价总数
     *
     * @return 评价总数
     */
    Integer getReviewTotal();

    /**
     * 根据ID列表批量获取评价
     *
     * @param sourceIdList 评价ID列表
     * @return 评价列表
     */
    List<Review> getReviewListByIds(List<Long> sourceIdList);

    /**
     * 批量更新评价点赞数
     *
     * @param updateMap 评价ID与点赞数的映射
     * @return 更新结果
     */
    Boolean updateLikeCountBatch(Map<Long, Integer> updateMap);
    /**
     * 批量更新评价子评论数
     *
     * @param updateMap 评价ID与子评论数的映射
     * @return 更新结果
     */
    Boolean updateCommentCountBatch(Map<Long, Integer> updateMap);
    /**
     * 批量更新评价收藏数
     *
     * @param updateMap 评价ID与收藏数的映射
     * @return 更新结果
     */
    Boolean updateStarCountBatch(Map<Long, Integer> updateMap);
    /**
     * 获取评价点赞数
     *
     * @param sourceId 评价ID
     * @return 点赞数量
     */
    Integer getReviewLikeCount(Long sourceId);
    /**
     * 根据ID获取评价详情
     *
     * @param id 评价主键
     * @return 评价实体
     */
    Review getReviewById(Long id);
    /**
     * 获取评价收藏数
     *
     * @param sourceId 评价ID
     * @return 收藏数量
     */
    Integer getReviewStarCount(Long sourceId);

    /**
     * 更新评价状态（审核通过/拒绝）
     *
     * @param id     评价ID
     * @param status 状态
     * @return 更新结果
     */
    Boolean updateReviewStatus(Long id, Integer status);

    /**
     * 判断当前用户是否评价过目标资源
     *
     * @param review 评价查询条件（至少包含 sourceType/sourceId）
     * @return 是否评价过
     */
    Boolean isReview(Review review);

    /**
     * 保存AI自动创建的评价
     *
     * @param reviews 评价列表
     * @return 保存结果
     */
    Boolean saveAiCreateReview(List<Review> reviews);
}
