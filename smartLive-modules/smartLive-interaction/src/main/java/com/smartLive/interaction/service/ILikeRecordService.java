package com.smartLive.interaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.domain.Collection;
import com.smartLive.interaction.domain.LikeRecord;
import com.smartLive.interaction.domain.vo.ResourceVO;
import com.smartLive.interaction.domain.vo.SocialInfoVO;

import java.util.List;


/**
 * 点赞记录Service接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface ILikeRecordService extends IService<LikeRecord> {
    /**
     * 点赞或取消点赞
     *
     * @param likeRecord
     * @return 点赞记录
     */
    Boolean likeOrCancelLike(LikeRecord likeRecord);

    /**
     * 查询点赞数
     *
     * @param
     * @return 点赞数
     */
    Integer queryLikeCount(LikeRecord likeRecord);

    /**
     * 查询点赞列表
     *
     * @param
     * @return 点赞记录
     */
    List<ResourceVO> queryLikeRecord(LikeRecord likeRecord,Integer current);
    /**
     * 查询点赞用户列表
     *
     * @param
     * @return 点赞用户列表
     */
    List<SocialInfoVO> queryLikeUserList(LikeRecord likeRecord);
}
