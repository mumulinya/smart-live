package com.smartLive.interaction.strategy;

import com.smartLive.common.core.constant.EsIndexNameConstants;
import com.smartLive.common.core.constant.mq.SearchMqConstants;
import com.smartLive.common.rabbitmq.domain.UserResourceMessage;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 互动操作（点赞、收藏、关注）的抽象策略模板类
 * 提供同步 ElasticSearch 的骨架方法，消除子类中重复的 MQ 投递代码
 */
public abstract class AbstractInteractionStrategy {

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    /**
     * 同步搜索资源数据到 ES（模板方法）
     * 只有需要同步搜索的资源（如博客、店铺、商品）实现子类需要调用此逻辑
     *
     * @param userId   操作的用户ID
     * @param sourceId 目标资源ID
     */
    public void syncUserResource(Long userId, Long sourceId) {
        // 1. 调用钩子方法，获取子类具体的业务数据对象
        Object data = getSourceData(sourceId);
        if (data == null) {
            return;
        }

        // 2. 调用钩子方法，获取业务前缀和行为常量
        String domain = getBizDomain();
        String actionType = getActionType();
        Integer sourceTypeCode = getType();

        // 3. 构建统一格式的 ES 文档 ID
        String id = userId + "_" + actionType + "_" + domain + "_" + sourceId;

        // 4. 封装统一的 UserResourceMessage
        UserResourceMessage userResourceMessage = UserResourceMessage.builder()
                .indexName(EsIndexNameConstants.USER_RESOURCE_INDEX_NAME)
                .id(id)
                .userId(userId)
                .sourceType(sourceTypeCode)
                .sourceId(sourceId)
                .actionType(actionType)
                .data(data)
                .build();

        // 5. 统一下发到 RabbitMQ
        MqMessageSendUtils.sendMqMessage(
                rabbitTemplate,
                SearchMqConstants.ES_EXCHANGE,
                SearchMqConstants.ES_ROUTING_USER_RESOURCE_INSERT,
                userResourceMessage
        );
    }

    /**
     * 获取业务资源对象实体（如 BlogDTO, ShopDTO）
     * @param sourceId 资源 ID
     * @return 业务 DTO/实体 对象
     */
    protected abstract Object getSourceData(Long sourceId);

    /**
     * 获取业务域前缀 (例如 GlobalBizTypeEnum.BLOG.getBizDomain())
     * @return 域字符串
     */
    protected abstract String getBizDomain();

    /**
     * 获取用户行为类型 (例如 UserResourceActionTypeConstants.USER_RESOURCE_ACTION_LIKE)
     * @return 行为字符串
     */
    protected abstract String getActionType();

    /**
     * 获取业务类型编码 (例如 ResourceTypeEnum.BLOG_RESOURCE.getCode())
     * 策略接口自带的方法，由子类实现
     * @return 类型编码
     */
    public abstract Integer getType();
}
