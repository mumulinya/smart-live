package com.smartLive.interaction.strategy.resource;

import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.interaction.domain.Comment;
import com.smartLive.interaction.service.ICommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class CommentResourceStrategy implements ResourceStrategy<Comment> {

    @Autowired
    private ICommentService iCommentService;
    /**
     * 策略标识 (USER / SHOP)
     */
    @Override
    public Integer getType() {
        return ResourceTypeEnum.COMMENT_RESOURCE.getCode();
    }

    /**
     * 获取资源列表
     *
     * @param sourceIdList
     */
    @Override
    public List<Comment> getResourceList(List<Long> sourceIdList) {
       List<Comment> commentList = iCommentService.getCommentListByIds(sourceIdList);
        if (commentList.isEmpty()) {
            return null;
        }
        return commentList;
    }
}
