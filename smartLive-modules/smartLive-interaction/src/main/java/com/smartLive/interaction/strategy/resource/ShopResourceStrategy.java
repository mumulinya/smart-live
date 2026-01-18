package com.smartLive.interaction.strategy.resource;

import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.blog.api.dto.BlogDto;
import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.interaction.domain.vo.ResourceVO;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.domain.ShopDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public  class ShopResourceStrategy implements ResourceStrategy {
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
    public List<ResourceVO> getResourceList(List<Long> sourceIdList) {

        List<ShopDTO> shopDTOList= remoteShopService.getShopList(sourceIdList);
        if (shopDTOList == null) {
            return null;
        }
        List<ResourceVO> resourceVOList = shopDTOList.stream().map(shopDTO -> ResourceVO.builder()
                .id(shopDTO.getId())
                .shopName(shopDTO.getName())
                .images(shopDTO.getImages())
                .area(shopDTO.getArea())
                .avgPrice(shopDTO.getAvgPrice())
                .comments(shopDTO.getComments())
                .score(shopDTO.getScore())
                .build()
        ).collect(Collectors.toList());
        return resourceVOList;
    }
}
