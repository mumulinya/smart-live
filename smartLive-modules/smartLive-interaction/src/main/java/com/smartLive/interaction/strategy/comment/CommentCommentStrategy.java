package com.smartLive.interaction.strategy.comment;

import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.interaction.service.ICommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class CommentCommentStrategy implements CommentStrategy {

    private final ICommentService commentService;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.COMMENT_RESOURCE.getCode();
    }

    @Override
    public void transCommentCountFromRedis2DB(Map<Long, Integer> updateMap) {
        // 调用博客服务的批量更新接口
        commentService.updateCommentCountBatch(updateMap);
    }
}