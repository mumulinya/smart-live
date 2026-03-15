package com.smartLive.interaction.strategy.comment;

import com.smartLive.blog.api.DTO.BlogDTO;
import com.smartLive.common.core.enums.common.ResourceTypeEnum;
import com.smartLive.interaction.domain.VO.CommentVO;
import com.smartLive.interaction.service.ICommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class CommentCommentStrategy implements CommentStrategy {

    private final ICommentService commentService;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.COMMENT_RESOURCE.getCode();
    }
    /**
     * 设置评价来源名称
     *
     * @param commentVOS
     * @return
     */
    @Override
    public List<CommentVO> setSourceName(List<CommentVO> commentVOS) {
        //获取源id集合
        List<Long> list = commentVOS.stream().map(CommentVO::getSourceId).distinct().toList();
        List<CommentVO> commentList = commentService.getCommentListByIds(list);
        if(commentList.isEmpty()){
            return commentVOS;
        }
        //将源id和源名称对应起来
        Map<Long, String> result = commentList.stream()
                .collect(Collectors.toMap(
                        CommentVO::getId,
                        CommentVO::getContent
                ));
        commentVOS.forEach(commentVO -> {
            commentVO.setSourceName(result.get(commentVO.getSourceId()));
        });
        return commentVOS;
    }
    @Override
    public void transCommentCountFromRedis2DB(Map<Long, Integer> updateMap) {
        // 调用博客服务的批量更新接口
        commentService.updateCommentCountBatch(updateMap);
    }
}