package com.smartLive.interaction.strategy.identity;

import com.smartLive.common.core.enums.IdentityTypeEnum;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.domain.ShopDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
@Component
public class ShopIdentityStrategy implements IdentityStrategy<ShopDTO> {

    @Autowired
    private RemoteShopService remoteShopService;
    /**
     * 策略标识 (USER / SHOP)
     */
    @Override
    public Integer getType() {
        return IdentityTypeEnum.SHOP_IDENTITY.getCode();
    }
    /**
     * 获取关注列表
     * @return
     */
    @Override
    public List<ShopDTO> getFollowList(List<Long> sourceIdList) {
        List<ShopDTO> shopList = remoteShopService.getShopList(sourceIdList);
        if (shopList == null || shopList.isEmpty()) {
            return null;
        }
        return shopList;
    }
}
