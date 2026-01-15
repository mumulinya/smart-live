package com.smartLive.interaction.strategy.star;

import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.common.core.enums.StarTypeEnum;
import com.smartLive.interaction.service.ICommentService;
import com.smartLive.shop.api.RemoteShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class ShopStarStrategy implements StarStrategy {

    private final RemoteShopService remoteShopService;

    @Override
    public Integer getType() {
        return ResourceTypeEnum.SHOP_RESOURCE.getCode();
    }

    @Override
    public void transStarCountFromRedis2DB(Map<Long, Integer> updateMap) {
        // 调用博客服务的批量更新接口
        remoteShopService.updateStarCountBatch(updateMap);
    }
}