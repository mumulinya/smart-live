package com.smartLive.interaction.strategy.resource;

import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.interaction.domain.VO.CommentVO;
import com.smartLive.interaction.service.ICommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Component
public class CommentResourceStrategy implements ResourceStrategy<CommentVO> {

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
    public List<CommentVO> getResourceList(List<Long> sourceIdList) {
        List<CommentVO> commentList = commentService.getCommentListByIds(sourceIdList);
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
    public CommentVO getResourceById(Long sourceId) {
        return commentService.getCommentById(sourceId);
    }

    /**
     * 获取资源id
     *
     * @param data
     * @return
     */
    @Override
    public Long getResourceId(CommentVO data) {
        return data.getId();
    }

    /**
     * 获取资源内容
     *
     * @param sourceId
     */
    @Override
    public HashMap<String,String> getResourceContentById(Long sourceId) {
        CommentVO comment = commentService.getCommentById(sourceId);
        HashMap<String, String> map = new HashMap<>();
        if (comment != null && comment.getContent() != null) {
            String content = comment.getContent();
            map.put("title", content.length() > 20 ? content.substring(0, 20) : content);
        }
        if (comment != null) {
            map.put("images", comment.getImages());
        }
        return map;
    }
}
