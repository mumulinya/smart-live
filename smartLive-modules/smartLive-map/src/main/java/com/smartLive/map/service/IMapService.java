package com.smartLive.map.service;

import com.smartLive.shop.api.domain.ShopDTO;
import com.smartLive.shop.api.domain.ShopTypeDTO;

import java.util.List;

public interface IMapService {
    /**
     * 查询商铺类型列表
     * @return
     */
    List<ShopTypeDTO> queryShopTypeList();

    /**
     * 根据条件查询商铺列表
     * @param shopDTO
     * @return
     */
    List<ShopDTO> queryShopList(ShopDTO shopDTO);
}
