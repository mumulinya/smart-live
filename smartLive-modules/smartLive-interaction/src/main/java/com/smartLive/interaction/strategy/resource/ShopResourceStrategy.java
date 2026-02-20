package com.smartLive.interaction.strategy.resource;

import com.smartLive.common.core.enums.ResourceTypeEnum;
import com.smartLive.interaction.domain.Comment;
import com.smartLive.interaction.domain.VO.ShopVO;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.DTO.ShopDTO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Component
public  class ShopResourceStrategy implements ResourceStrategy<ShopVO> {
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
    public List<ShopVO> getResourceList(List<Long> sourceIdList) {

        List<ShopDTO> shopDTOList= remoteShopService.getShopList(sourceIdList);
        if (shopDTOList == null) {
            return null;
        }
        List<ShopVO> shopVOList = shopDTOList.stream().map(shopDTO -> {
            ShopVO shopVO = new ShopVO();
            BeanUtils.copyProperties(shopDTO, shopVO);
            if(shopDTO.getAvgPrice() != null) {
                shopVO.setAvgPrice(String.valueOf(shopDTO.getAvgPrice()));
            }
            if(shopDTO.getReviews() != null) {
                shopVO.setComments(shopDTO.getReviews());
            }
            return shopVO;
        }).collect(Collectors.toList());
        return shopVOList;
    }

    /**
     * 获取资源
     *
     * @param sourceId
     */
    @Override
    public ShopVO getResourceById(Long sourceId) {
        ShopDTO shop = remoteShopService.getShopById(sourceId);
        if (shop == null) {
            return null;
        }
        ShopVO shopVO = new ShopVO();
        BeanUtils.copyProperties(shop, shopVO);
        if(shop.getAvgPrice() != null) {
            shopVO.setAvgPrice(String.valueOf(shop.getAvgPrice()));
        }
        if(shop.getReviews() != null) {
            shopVO.setComments(shop.getReviews());
        }
        return shopVO;
    }

    /**
     * 获取资源id
     *
     * @param data
     * @return
     */
    @Override
    public Long getResourceId(ShopVO data) {
        return data.getId();
    }

    /**
     * 获取资源内容
     *
     * @param sourceId
     */
    @Override
    public HashMap<String,String> getResourceContentById(Long sourceId) {
        ShopDTO shop = remoteShopService.getShopById(sourceId);
        HashMap<String, String> map = new HashMap<>();
        map.put("title", shop.getName());
        map.put("images", shop.getImages());
        return map;
    }
}
