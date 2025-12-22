package com.smartLive.interaction.strategy.like;

import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("blogLikeStrategy")
@RequiredArgsConstructor
public class BlogLikeStrategy implements LikeStrategy {

    private final RemoteBlogService remoteBlogService;

    @Override
    public String getType() {
        return ResourceTypeEnum.BLOG_RESOURCE.getBizDomain()+"LikeStrategy";
    }

    @Override
    public void transLikeCountFromRedis2DB(Map<Long, Integer> updateMap) {
        // 调用博客服务的批量更新接口
        remoteBlogService.updateLikeCountBatch(updateMap);
    }
}