package com.smartLive.interaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.interaction.api.DTO.LikeDTO;
import com.smartLive.interaction.domain.Like;
import com.smartLive.interaction.domain.VO.LikeVO;
import java.util.List;
import java.util.Map;


/**
 * 点赞记录Service接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface ILikeService extends IService<Like> {
    /**
     * 点赞或取消点赞
     *
     * @param like 点赞实体
     * @return 操作结果
     */
    Boolean likeOrCancelLike(Like like);

    /**
     * 查询点赞数
     *
     * @param like 点赞查询条件
     * @return 点赞数
     */
    Integer queryLikeCount(Like like);

    /**
     * 查询点赞列表（分页）
     *
     * @param like    点赞查询条件
     * @param current 当前页码
     * @return 点赞记录列表
     */
    List<LikeVO> queryLikeRecord(Like like, Integer current);
    /**
     * 查询点赞用户列表
     *
     * @param like 点赞查询条件
     * @return 点赞用户列表
     */
    List<?> queryLikeUserList(Like like);
    /**
     * 判断是否已点赞
     *
     * @param like 点赞查询条件
     * @return 是否已点赞
     */
    Boolean isLike(Like like);
    /**
     * 获取用户点赞总数
     *
     * @param like 点赞查询条件
     * @return 点赞总数
     */
    Integer getUserLikeCount(Like like);
    /**
     * 批量判断是否已点赞
     *
     * @param likeDTO   点赞查询条件（含用户ID和来源类型）
     * @param sourceIds 目标资源ID列表
     * @return 资源ID与是否点赞的映射
     */
    Map<Long, Boolean> isLikeBatch(LikeDTO likeDTO, List<Long> sourceIds);
}
