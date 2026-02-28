package com.smartLive.interaction.strategy.follow;

import com.smartLive.common.core.constant.EsIndexNameConstants;
import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.core.constant.UserResourceActionTypeConstants;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.common.rabbitmq.domain.UserResourceMessage;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.product.api.DTO.ProductDTO;
import com.smartLive.product.api.RemoteProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class ProductFollowStrategy implements FollowStrategy {

    private final RemoteProductService remoteProductService;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.PRODUCT_RESOURCE.getCode();
    }

    @Override
    public void transFansCountFromRedis2DB(Map<Long, Integer> updateMap) {
        log.info("正在调用商品服务，同步数据");
        // 调用商品服务的批量更新接口
        Boolean b = remoteProductService.updateFansCountBatch(updateMap);
        if (b) {
            log.info("同步数据成功");
        } else {
            log.info("同步数据失败");
        }
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
        ProductDTO product = remoteProductService.getProductById(sourceId);
        //文档id
        String actionType=UserResourceActionTypeConstants.USER_RESOURCE_ACTION_FOLLOW;
        String  id = userId+"_"+actionType+"_"+GlobalBizTypeEnum.PRODUCT.getBizDomain()+"_"+product.getId().toString();
        UserResourceMessage userResourceMessage = UserResourceMessage.builder()
                .indexName(EsIndexNameConstants.USER_RESOURCE_INDEX_NAME)
                .id(id)
                .userId(userId)
                .sourceType(ResourceTypeEnum.PRODUCT_RESOURCE.getCode())
                .sourceId(sourceId)
                .actionType(actionType)
                .data(product)
                .build();
        //发送消息
        MqMessageSendUtils.sendMqMessage(rabbitTemplate, MqConstants.ES_EXCHANGE,MqConstants.ES_ROUTING_USER_RESOURCE_INSERT, userResourceMessage);
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

    /**
     * 从商品表获取粉丝数
     *
     * @param sourceId 商品 ID
     * @return 粉丝数
     */
    @Override
    public Integer getFanCount(Long sourceId) {
        ProductDTO product = remoteProductService.getProductById(sourceId);
        return product != null ? product.getFans() : null;
    }
}
