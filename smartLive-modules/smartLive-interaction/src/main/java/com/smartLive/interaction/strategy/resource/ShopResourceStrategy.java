package com.smartLive.interaction.strategy.resource;

import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.DTO.ShopDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public  class ShopResourceStrategy implements ResourceStrategy<ShopDTO> {
    @Autowired
    private RemoteShopService remoteShopService;
    /**
     * 策略标识 (USER / SHOP)
     */
    @Override
    public Integer getType() {
        return ResourceTypeEnum.SHOP_RESOURCE.getCode();
    }

    /**
     * 获取资源列表
     *
     * @param sourceIdList
     */
    @Override
    public List<ShopDTO> getResourceList(List<Long> sourceIdList) {

        List<ShopDTO> shopDTOList= remoteShopService.getShopList(sourceIdList);
        if (shopDTOList == null) {
            return null;
        }
        return shopDTOList;
    }
}
