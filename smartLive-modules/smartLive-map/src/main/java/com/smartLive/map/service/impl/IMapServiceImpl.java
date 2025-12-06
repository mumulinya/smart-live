package com.smartLive.map.service.impl;

import com.smartLive.common.core.domain.R;
import com.smartLive.map.service.IMapService;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.domain.ShopDTO;
import com.smartLive.shop.api.domain.ShopTypeDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@Slf4j
public class IMapServiceImpl implements IMapService {

    @Autowired
    RemoteShopService remoteShopService;
    /**
     * 查询商铺类型列表
     *
     * @return
     */
    @Override
    public List<ShopTypeDTO> queryShopTypeList() {
        R<List<ShopTypeDTO>> shopTypeListData = remoteShopService.getShopTypeList();
        return shopTypeListData.getData();
    }

    /**
     * 根据条件查询商铺列表
     *
     * @param shopDTO
     * @return
     */
    @Override
    public List<ShopDTO> queryShopList(ShopDTO shopDTO) {
        R<List<ShopDTO>> shopListData = remoteShopService.queryShopList(shopDTO);
        if (R.isError(shopListData)) {
            log.error("查询商铺列表失败: {}", shopListData.getMsg());
            return null;
        }
        List<ShopDTO> data = shopListData.getData();
        return data;
    }
}
