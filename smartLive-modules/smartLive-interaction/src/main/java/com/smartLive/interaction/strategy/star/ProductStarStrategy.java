package com.smartLive.interaction.strategy.star;
import com.smartLive.common.core.constant.mq.SearchMqConstants;

import com.smartLive.common.core.constant.EsIndexNameConstants;
import com.smartLive.common.core.constant.UserResourceActionTypeConstants;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.common.rabbitmq.domain.UserResourceMessage;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.product.api.DTO.ProductDTO;
import com.smartLive.product.api.RemoteProductService;
import com.smartLive.interaction.strategy.AbstractInteractionStrategy;
import com.smartLive.common.core.constant.UserResourceActionTypeConstants;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.product.api.RemoteProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class ProductStarStrategy extends AbstractInteractionStrategy implements StarStrategy{

    private final RemoteProductService remoteProductService;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.PRODUCT_RESOURCE.getCode();
    }
    /**
     * 同步数据到DB
     *
     * @param updateMap
     */
    @Override
    public void transStarCountFromRedis2DB(Map<Long, Integer> updateMap) {
        log.info("正在调用商品服务，同步数据");
        // 调用商品服务的批量更新接口
        remoteProductService.updateStarCountBatch(updateMap);
    }

    @Override
    protected Object getSourceData(Long sourceId) {
        return remoteProductService.getProductById(sourceId);
    }

    @Override
    protected String getBizDomain() {
        return GlobalBizTypeEnum.PRODUCT.getBizDomain();
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
        return remoteProductService.getProductStarCount(sourceId);
    }
}