package com.smartLive.interaction.strategy.like;

import com.smartLive.common.core.constant.UserResourceActionTypeConstants;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.common.ResourceTypeEnum;
import com.smartLive.interaction.service.ICommentService;
import com.smartLive.interaction.strategy.AbstractInteractionStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 评论点赞策略实现
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class CommentLikeStrategy extends AbstractInteractionStrategy implements LikeStrategy {

    private final ICommentService commentService;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.COMMENT_RESOURCE.getCode();
    }

    @Override
    public void transLikeCountFromRedis2DB(Map<Long, Integer> updateMap) {
        if (updateMap == null || updateMap.isEmpty()) {
            return;
        }

        try {
            Boolean result = commentService.updateLikeCountBatch(updateMap);
            if (!result) {
                throw new RuntimeException("评论点赞数更新失败");
            }
        } catch (Exception e) {
            log.error("评论点赞数同步失败", e);
        }
    }

    @Override
    public Integer getLikeCount(Long sourceId) {
        if (sourceId == null || sourceId <= 0) {
            return 0;
        }

        try {
            return commentService.getCommentLikeCount(sourceId);
        } catch (Exception e) {
            log.error("获取评论点赞数异常: sourceId={}", sourceId, e);
            return 0;
        }
    }

    @Override
    protected Object getSourceData(Long sourceId) {
        return commentService.getCommentById(sourceId);
    }

    @Override
    protected String getBizDomain() {
        return GlobalBizTypeEnum.COMMENT.getBizDomain();
    }

    @Override
    protected String getActionType() {
        return UserResourceActionTypeConstants.USER_RESOURCE_ACTION_LIKE;
    }
}
