package com.smartLive.interaction.strategy.resource;

import com.smartLive.blog.api.DTO.BlogDTO;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.interaction.domain.Comment;
import com.smartLive.interaction.service.ICommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Component
public class CommentResourceStrategy implements ResourceStrategy<Comment> {

    @Autowired
    private ICommentService commentService;

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
        List<Comment> commentList = commentService.getCommentListByIds(sourceIdList);
        if (commentList.isEmpty()) {
            return null;
        }
        return commentList;
    }

    /**
     * 获取资源
     *
     * @param sourceId
     */
    @Override
    public Comment getResourceById(Long sourceId) {
        Comment comment = commentService.getCommentById(sourceId);
        return comment;
    }

    /**
     * 获取资源内容
     *
     * @param sourceId
     */
    @Override
    public HashMap<String,String> getResourceContentById(Long sourceId) {
        Comment comment = commentService.getCommentById(sourceId);
        HashMap<String, String> map = new HashMap<>();
        map.put("title", comment.getContent());
        map.put("images", comment.getImages());
        return map;
    }
}
