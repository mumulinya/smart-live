package com.smartLive.ai.service.strategy.milvus.Impl;

import com.smartLive.ai.entity.DOC.ShopDoc;
import com.smartLive.ai.service.business.IShopMilvusService;
import com.smartLive.ai.service.strategy.milvus.MilvusSyncStrategy;
import com.smartLive.ai.utils.EsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
@Component
public class ShopMilvusStrategy implements MilvusSyncStrategy {
    /**
     * 获取策略支持的数据类型，例如 "voucher", "shop"
     */
    @Override
    public String getDataType() {
        return "shop";
    }

    @Autowired
    private IShopMilvusService shopMilvusService;

    @Override
    public boolean insertOrUpdate(String id, Object rawData) throws IOException {
        // 1. 策略自己知道要把 Map 转成什么实体类，Listener 不需要知道
        ShopDoc doc = EsTool.convertToObject((Map) rawData, ShopDoc.class);
        // 2. 调用业务 Service
        return shopMilvusService.insertOrUpdate(id, doc);
    }

    @Override
    public boolean batchInsert(List<Object> rawDataList) throws IOException {
        List<ShopDoc> list = EsTool.convertList(rawDataList, ShopDoc.class);
        return shopMilvusService.batchInsert(list);
    }

    @Override
    public boolean delete(String id) throws IOException {
        return shopMilvusService.delete(id);
    }
}
