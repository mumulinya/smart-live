package com.smartLive.interaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.interaction.domain.Review;
import com.smartLive.interaction.domain.VO.BadReviewVO;
import com.smartLive.interaction.domain.VO.ReviewVO;
import com.smartLive.interaction.domain.VO.ShopReviewAnalysisVO;
import com.smartLive.interaction.domain.VO.ShopReviewSuggestVO;

import java.util.List;
import java.util.Map;

/**
 * 评价 Service 接口。
 *
 * @author mumulin
 * @date 2025-09-21
 */
public interface IReviewService extends IService<Review> {

    /**
     * 根据主键查询评价。
     *
     * @param id 评价主键
     * @return 评价实体
     */
    ReviewVO selectReviewById(Long id);

    /**
     * 按条件查询评价列表（管理端）。
     *
     * @param review 查询条件
     * @return 评价视图列表
     */
    List<ReviewVO> selectReviewList(Review review);

    /**
     * 新增评价。
     *
     * @param review 评价实体
     * @return 影响行数
     */
    int insertReview(Review review);

    /**
     * 修改评价。
     *
     * @param review 评价实体
     * @return 影响行数
     */
    int updateReview(Review review);

    /**
     * 批量删除评价。
     *
     * @param ids 评价主键数组
     * @return 影响行数
     */
    int deleteReviewByIds(Long[] ids);

    /**
     * 根据主键删除评价。
     *
     * @param id 评价主键
     * @return 影响行数
     */
    int deleteReviewById(Long id);

    /**
     * 分页查询评价列表（前台）。
     *
     * @param review 查询条件
     * @param current 当前页
     * @param sort 排序方式：latest（最新）/hot（最热）
     * @return 评价视图列表
     */
    List<ReviewVO> listReview(Review review, Integer current, String sort);

    /**
     * 用户新增评价。
     *
     * @param review 评价实体
     * @return 评价 ID
     */
    Integer addReview(Review review);

    /**
     * 用户删除评价。
     *
     * @param review 评价实体（包含必要身份与资源信息）
     * @return 是否删除成功
     */
    Boolean deleteReview(Review review);

    /**
     * 查询用户自己的评价列表。
     *
     * @param review 查询条件
     * @param current 当前页
     * @return 评价视图列表
     */
    List<ReviewVO> getReviewOfUser(Review review, Integer current);

    /**
     * 查询评价数量。
     *
     * @param review 查询条件
     * @return 数量
     */
    Integer getReviewCount(Review review);

    /**
     * 查询评价总数。
     *
     * @return 总数
     */
    Integer getReviewTotal();

    /**
     * 按评价 ID 列表批量查询评价。
     *
     * @param sourceIdList 评价 ID 列表
     * @return 评价视图列表
     */
    List<ReviewVO> getReviewListByIds(List<Long> sourceIdList);

    /**
     * 批量更新评价点赞数。
     *
     * @param updateMap 评价 ID 与点赞数映射
     * @return 是否更新成功
     */
    Boolean updateLikeCountBatch(Map<Long, Integer> updateMap);

    /**
     * 批量更新评价子评论数。
     *
     * @param updateMap 评价 ID 与子评论数映射
     * @return 是否更新成功
     */
    Boolean updateCommentCountBatch(Map<Long, Integer> updateMap);

    /**
     * 批量更新评价收藏数。
     *
     * @param updateMap 评价 ID 与收藏数映射
     * @return 是否更新成功
     */
    Boolean updateStarCountBatch(Map<Long, Integer> updateMap);

    /**
     * 查询评价点赞数。
     *
     * @param sourceId 评价 ID
     * @return 点赞数
     */
    Integer getReviewLikeCount(Long sourceId);

    /**
     * 根据主键查询评价详情。
     *
     * @param id 评价主键
     * @return 评价视图对象
     */
    ReviewVO getReviewById(Long id);

    /**
     * 查询评价收藏数。
     *
     * @param sourceId 评价 ID
     * @return 收藏数
     */
    Integer getReviewStarCount(Long sourceId);

    /**
     * 更新评价审核状态。
     *
     * @param id 评价 ID
     * @param status 审核状态
     * @param reason 拒绝原因（通过时可为 null）
     * @return 是否更新成功
     */
    Boolean updateReviewStatus(Long id, Integer status, String reason);

    /**
     * 判断当前用户是否评价过目标资源。
     *
     * @param review 查询条件（至少包含 sourceType/sourceId）
     * @return true 表示已评价
     */
    Boolean isReview(Review review);

    /**
     * 保存 AI 生成的评价。
     *
     * @param reviews 评价列表
     * @return 是否保存成功
     */
    Boolean saveAiCreateReview(List<Review> reviews);

    /**
     * 全量发布评价到向量库。
     *
     * @return 发布结果
     */
    String allPublish();

    /**
     * 按 ID 发布评价到向量库。
     *
     * @param ids 评价 ID 数组
     * @return 发布结果
     */
    String publish(String[] ids);
    ShopReviewAnalysisVO getShopReviewAnalysis(Long shopId, String startTime, String endTime);

    ShopReviewSuggestVO getShopReviewSuggest(Long shopId, String timeRange);

    ShopReviewSuggestVO getShopReviewSuggestRealtime(Long shopId);
}
