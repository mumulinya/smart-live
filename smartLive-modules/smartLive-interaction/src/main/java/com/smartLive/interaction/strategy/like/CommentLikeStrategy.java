package com.smartLive.interaction.strategy.like;

import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.interaction.service.ICommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("commentLikeStrategy")
@RequiredArgsConstructor
public class CommentLikeStrategy implements LikeStrategy {

    private final ICommentService commentService;

    @Override
    public String getType() {
        return ResourceTypeEnum.COMMENT_RESOURCE.getBizDomain()+"LikeStrategy";
    }

    @Override
    public void transLikeCountFromRedis2DB(Map<Long, Integer> updateMap) {
        // 调用博客服务的批量更新接口
        commentService.updateLikeCountBatch(updateMap);
    }
}