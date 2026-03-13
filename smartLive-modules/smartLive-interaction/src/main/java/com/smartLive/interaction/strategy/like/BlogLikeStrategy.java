package com.smartLive.interaction.strategy.like;

import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.common.core.constant.UserResourceActionTypeConstants;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.common.ResourceTypeEnum;
import com.smartLive.interaction.strategy.AbstractInteractionStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class BlogLikeStrategy extends AbstractInteractionStrategy implements LikeStrategy {

    private final RemoteBlogService remoteBlogService;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.BLOG_RESOURCE.getCode();
    }

    @Override
    public void transLikeCountFromRedis2DB(Map<Long, Integer> updateMap) {
        log.info("正在调用博客服务，同步数据");
        // 调用博客服务的批量更新接口
        Boolean b = remoteBlogService.updateLikeCountBatch(updateMap);
        if (b) {
            log.info("同步数据成功");
        } else {
            log.info("同步数据失败");
        }
    }

    @Override
    protected Object getSourceData(Long sourceId) {
        return remoteBlogService.getBlogById(sourceId);
    }

    @Override
    protected String getBizDomain() {
        return GlobalBizTypeEnum.BLOG.getBizDomain();
    }

    @Override
    protected String getActionType() {
        return UserResourceActionTypeConstants.USER_RESOURCE_ACTION_LIKE;
    }

    /**
     * 获取点赞数
     *
     * @param sourceId 业务ID
     * @return 点赞数
     */
    @Override
    public Integer getLikeCount(Long sourceId) {
        return remoteBlogService.getBlogLikeCount(sourceId);
    }
}