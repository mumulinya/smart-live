package com.smartLive.interaction.strategy.star;

import com.smartLive.blog.api.DTO.BlogDTO;
import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.common.core.constant.EsIndexNameConstants;
import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.common.rabbitmq.domain.UserResourceMessage;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.marketing.api.DTO.VoucherDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class BlogStarStrategy implements StarStrategy {

    private final RemoteBlogService remoteBlogService;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.BLOG_RESOURCE.getCode();
    }
    /**
     * 同步收藏数据到DB
     *
     * @param updateMap
     */
    @Override
    public void transStarCountFromRedis2DB(Map<Long, Integer> updateMap) {
        log.info("正在调用博客服务，同步数据");
        // 调用博客服务的批量更新接口
        Boolean b = remoteBlogService.updateStarCountBatch(updateMap);
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
    public void syncUserResource(Long userId,Long sourceId) {
        BlogDTO blog = remoteBlogService.getBlogById(sourceId);
        UserResourceMessage userResourceMessage = UserResourceMessage.builder()
                .indexName(EsIndexNameConstants.USER_RESOURCE_INDEX_NAME)
                .id(GlobalBizTypeEnum.BLOG.getBizDomain()+"_"+sourceId)
                .userId(userId)
                .sourceType(ResourceTypeEnum.VOUCHER_RESOURCE.getCode())
                .sourceId(sourceId)
                .actionType("star")
                .data(blog)
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
        return remoteBlogService.getStarCount(sourceId);
    }
}