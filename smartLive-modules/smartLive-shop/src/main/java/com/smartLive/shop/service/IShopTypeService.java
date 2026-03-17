package com.smartLive.shop.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.shop.domain.ShopType;

/**
 * 店铺类型服务接口。
 */
public interface IShopTypeService extends IService<ShopType> {

    /**
     * 查询店铺类型列表。
     */
    Result queryList();
}
