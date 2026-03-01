package com.smartLive.interaction.strategy.follow;
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
public class ProductFollowStrategy extends AbstractInteractionStrategy implements FollowStrategy {

    private final RemoteProductService remoteProductService;

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
