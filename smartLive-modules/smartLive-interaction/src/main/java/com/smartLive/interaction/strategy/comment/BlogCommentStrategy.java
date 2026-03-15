package com.smartLive.interaction.strategy.comment;

import com.smartLive.blog.api.DTO.BlogDTO;
import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.common.core.enums.common.ResourceTypeEnum;
import com.smartLive.interaction.domain.VO.CommentVO;
import com.smartLive.interaction.domain.VO.ReviewVO;
import com.smartLive.product.api.DTO.ProductDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Slf4j
public class BlogCommentStrategy implements CommentStrategy {

    private final RemoteBlogService remoteBlogService;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.BLOG_RESOURCE.getCode();
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
        List<BlogDTO> blogList = remoteBlogService.getBlogListByIds(list);
        if(blogList == null || blogList.isEmpty()) {
            return commentVOS;
        }
        //将源id和源名称对应起来
        Map<Long, String> result = blogList.stream()
                .collect(Collectors.toMap(
                        BlogDTO::getId,
                        BlogDTO::getTitle
                ));
        commentVOS.forEach(commentVO -> {
            commentVO.setSourceName(result.get(commentVO.getSourceId()));
        });
        return commentVOS;
    }

    @Override
    public void transCommentCountFromRedis2DB(Map<Long, Integer> updateMap) {
        // 调用博客服务的批量更新接口
        Boolean b = remoteBlogService.updateCommentCountBatch(updateMap);
        if(b){
            log.info("更新成功{}", updateMap);
        }else{
            log.info("更新失败{}", updateMap);
        }
    }
}