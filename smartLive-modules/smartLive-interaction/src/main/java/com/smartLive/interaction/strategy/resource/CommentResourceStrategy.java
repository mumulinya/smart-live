package com.smartLive.interaction.strategy.resource;

import com.smartLive.blog.api.dto.BlogDto;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.interaction.domain.Comment;
import com.smartLive.interaction.domain.vo.ResourceVO;
import com.smartLive.interaction.service.ICollectionService;
import com.smartLive.interaction.service.ICommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component("commentResource")
public abstract  class CommentResourceStrategy implements ResourceFetcherStrategy{

    @Autowired
    private ICommentService iCommentService;
    /**
     * 策略标识 (USER / SHOP)
     */
    @Override
    public String getType() {
        return ResourceTypeEnum.COMMENTRESOURCE.getStrategyName();
    }

    /**
     * 获取资源列表
     *
     * @param sourceIdList
     */
    @Override
    public List<ResourceVO> getResourceList(List<Long> sourceIdList) {
       List<Comment> commentList = iCommentService.getCommentListByIds(sourceIdList);
        if (commentList.isEmpty()) {
            return null;
        }
        List<ResourceVO> resourceVOList = commentList.stream().map(comment -> ResourceVO.builder()
                .id(comment.getId())
                .userAvatar(comment.getUserIcon())
                .userName(comment.getNickName())
                .cover(comment.getImages())
                .content(comment.getContent())
                .isLike(true)
                .build()
        ).collect(Collectors.toList());
        return resourceVOList;
    }
}
