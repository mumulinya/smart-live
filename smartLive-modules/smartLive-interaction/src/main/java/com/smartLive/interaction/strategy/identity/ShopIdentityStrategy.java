package com.smartLive.interaction.strategy.identity;

import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.enums.IdentityTypeEnum;
import com.smartLive.interaction.domain.vo.SocialInfoVO;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.domain.ShopDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component("shopIdentityStrategy")
public class ShopIdentityStrategy implements IdentityStrategy<ShopDTO> {

    @Autowired
    private RemoteShopService remoteShopService;
    /**
     * 策略标识 (USER / SHOP)
     */
    @Override
    public String getType() {
        return IdentityTypeEnum.SHOP_IDENTITY.getBizDomain()+"IdentityStrategy";
    }
    /**
     * 获取关注列表
     * @return
     */
    @Override
    public List<SocialInfoVO> getFollowList(List<Long> sourceIdList) {
        R<List<ShopDTO>> r = remoteShopService.getShopList(sourceIdList);
        if (r.getCode() != 200) {
            return null;
        }
        List<ShopDTO> shopList = r.getData();
        List<SocialInfoVO> socialInfoVOList = shopList.stream().map(shop -> SocialInfoVO.builder()
                .isFollow(true)
                .id(shop.getId())
                .name(shop.getName())
                .avatar(shop.getImages())
                .description(shop.getRemark())
                .build()
        ).collect(Collectors.toList());
        return socialInfoVOList;
    }
}
