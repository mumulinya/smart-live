package com.smartLive.interaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.interaction.domain.Like;
import com.smartLive.interaction.domain.vo.ResourceVO;
import com.smartLive.interaction.domain.vo.SocialInfoVO;

import java.util.List;


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
     * @param like
     * @return 点赞记录
     */
    Boolean likeOrCancelLike(Like like);

    /**
     * 查询点赞数
     *
     * @param
     * @return 点赞数
     */
    Integer queryLikeCount(Like like);

    /**
     * 查询点赞列表
     *
     * @param
     * @return 点赞记录
     */
    List<ResourceVO> queryLikeRecord(Like like, Integer current);
    /**
     * 查询点赞用户列表
     *
     * @param
     * @return 点赞用户列表
     */
    List<SocialInfoVO> queryLikeUserList(Like like);
    /**
     * 判断是否点赞
     *
     * @param
     * @return 是否点赞
     */
    Boolean isLike(Like like);
}
