package com.smartLive.interaction.strategy.follow;
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
public class ShopFollowStrategy extends AbstractInteractionStrategy implements FollowStrategy {

    private final RemoteShopService remoteShopService;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.SHOP_RESOURCE.getCode();
    }

    @Override
    public void transFansCountFromRedis2DB(Map<Long, Integer> updateMap) {
        // 调用博客服务的批量更新接口
        remoteShopService.updateFansCountBatch(updateMap);
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
        return UserResourceActionTypeConstants.USER_RESOURCE_ACTION_FOLLOW;
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

    /**
     * 从店铺表获取粉丝数
     *
     * @param sourceId 店铺 ID
     * @return 粉丝数
     */
    @Override
    public Integer getFanCount(Long sourceId) {
        ShopDTO shop = remoteShopService.getShopById(sourceId);
        return shop != null ? shop.getFans() : null;
    }
}
