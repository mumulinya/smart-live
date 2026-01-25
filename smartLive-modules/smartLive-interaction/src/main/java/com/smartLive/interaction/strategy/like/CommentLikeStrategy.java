package com.smartLive.interaction.strategy.like;

import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.interaction.service.ICommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class CommentLikeStrategy implements LikeStrategy {

    private final ICommentService commentService;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.COMMENT_RESOURCE.getCode();
    }

    @Override
    public void transLikeCountFromRedis2DB(Map<Long, Integer> updateMap) {
        // 调用博客服务的批量更新接口
        commentService.updateLikeCountBatch(updateMap);
    }

    /**
     * 获取点赞数
     *
     * @param sourceId 业务ID
     * @return 点赞数
     */
    @Override
    public Integer getLikeCount(Long sourceId) {
        return commentService.getCommentLikeCount(sourceId);
    }
}