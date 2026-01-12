package com.smartLive.interaction.strategy.comment;

import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class BlogCommentStrategy implements CommentStrategy {

    private final RemoteBlogService remoteBlogService;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.BLOG_RESOURCE.getCode();
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