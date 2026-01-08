package com.smartLive.search.strategy;

import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.search.domain.ShopDoc;
import com.smartLive.search.service.IShopEsService;
import com.smartLive.search.utils.EsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class ShopEsStrategy implements EsSyncStrategy {
    @Autowired
    IShopEsService shopEsService;

    /**
     * 获取策略的类型
     */
    @Override
    public Integer getType() {
        return GlobalBizTypeEnum.SHOP.getCode();
    }

    /**
     * 单条插入或更新
     *
     * @param indexName
     * @param id
     * @param data
     * @return
     * @throws IOException
     */
    @Override
    public boolean insertOrUpdate(String indexName, String id, Object data) throws IOException {
        ShopDoc doc = EsTool.convertToObject((Map) data, ShopDoc.class);
        return shopEsService.insertOrUpdate(indexName, id, doc);
    }

    /**
     * 批量插入
     *
     * @param indexName   ES索引名
     * @param dataList    实体列表
     * @return 是否成功
     */
    @Override
    public boolean batchInsert(String indexName, List<Object> dataList) throws IOException {
        List<ShopDoc> docList = EsTool.convertList(dataList, ShopDoc.class);
        return shopEsService.batchInsert(indexName, docList,  data -> data.getId().toString());
    }

    /**
     * 按ID删除
     *
     * @param indexName ES索引名
     * @param id        文档ID
     * @return 是否成功
     */
    @Override
    public boolean delete(String indexName, String id) throws IOException {
        return shopEsService.delete(indexName,id);
    }
}
