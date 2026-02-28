package com.smartLive.interaction.strategy.star;
import com.smartLive.common.core.constant.mq.SearchMqConstants;

import com.smartLive.common.core.constant.EsIndexNameConstants;
import com.smartLive.common.core.constant.UserResourceActionTypeConstants;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.common.rabbitmq.domain.UserResourceMessage;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.shop.api.DTO.ShopDTO;
import com.smartLive.shop.api.RemoteShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class ShopStarStrategy implements StarStrategy{

    private final RemoteShopService remoteShopService;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.SHOP_RESOURCE.getCode();
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
     * 同步数据到ES
     * 使用 default 关键字提供默认空实现
     * 只有需要同步搜索的资源（如博客、店铺）才需要重写此方法
     *
     * @param
     */
    @Override
    public void syncUserResource(Long userId, Long sourceId) {
        ShopDTO shop = remoteShopService.getShopById(sourceId);
        String actionType=UserResourceActionTypeConstants.USER_RESOURCE_ACTION_STAR;
        String  id = userId+"_"+actionType+"_"+GlobalBizTypeEnum.SHOP.getBizDomain()+"_"+shop.getId().toString();
        UserResourceMessage userResourceMessage = UserResourceMessage.builder()
                .indexName(EsIndexNameConstants.USER_RESOURCE_INDEX_NAME)
                .id(id)
                .userId(userId)
                .sourceType(ResourceTypeEnum.SHOP_RESOURCE.getCode())
                .sourceId(sourceId)
                .actionType(actionType)
                .data(shop)
                .build();
        //发送消息
        MqMessageSendUtils.sendMqMessage(rabbitTemplate, SearchMqConstants.ES_EXCHANGE,SearchMqConstants.ES_ROUTING_USER_RESOURCE_INSERT, userResourceMessage);
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