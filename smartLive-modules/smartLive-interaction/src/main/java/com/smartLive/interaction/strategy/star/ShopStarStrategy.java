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
import com.smartLive.interaction.strategy.AbstractInteractionStrategy;
import com.smartLive.common.core.constant.UserResourceActionTypeConstants;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.shop.api.RemoteShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class ShopStarStrategy extends AbstractInteractionStrategy implements StarStrategy{

    private final RemoteShopService remoteShopService;

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

    @Override
    protected Object getSourceData(Long sourceId) {
        return remoteShopService.getShopById(sourceId);
    }

    @Override
    protected String getBizDomain() {
        return GlobalBizTypeEnum.SHOP.getBizDomain();
    }

    @Override
    protected String getActionType() {
        return UserResourceActionTypeConstants.USER_RESOURCE_ACTION_STAR;
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