package com.smartLive.interaction.strategy.follow;

import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.shop.api.RemoteShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class UserFollowStrategy implements FollowStrategy {

    private final RemoteShopService remoteShopService;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.USER_RESOURCE.getCode();
    }
    /**
     * 同步收藏数据到DB
     *
     * @param updateMap
     */
    @Override
    public void transStarCountFromRedis2DB(Map<Long, Integer> updateMap) {
        // 调用博客服务的批量更新接口
        remoteShopService.updateStarCountBatch(updateMap);
    }

    /**
     * 获取收藏数
     *
     * @param sourceId 业务ID
     * @return 点赞数
     */
    @Override
    public Integer getStarCount(Long sourceId) {
        return remoteShopService.getStarCount(sourceId);
    }
}