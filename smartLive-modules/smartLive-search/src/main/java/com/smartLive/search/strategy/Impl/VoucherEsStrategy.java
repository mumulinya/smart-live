package com.smartLive.search.strategy.Impl;

import com.smartLive.search.domain.VoucherDoc;
import com.smartLive.search.service.IVoucherEsService;
import com.smartLive.search.strategy.EsSyncStrategy;
import com.smartLive.search.utils.EsTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component("voucher")
public class VoucherEsStrategy implements EsSyncStrategy {
    @Autowired
    IVoucherEsService voucherEsService;

    /**
     * 获取策略支持的数据类型，例如 "voucher", "voucher"
     */
    @Override
    public String getDataType() {
        return "voucher";
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
        VoucherDoc doc = EsTool.convertToObject((Map) data, VoucherDoc.class);
        return voucherEsService.insertOrUpdate(indexName, id, doc);
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
        List<VoucherDoc> docList = EsTool.convertList(dataList, VoucherDoc.class);
        return voucherEsService.batchInsert(indexName, docList,  data -> data.getId().toString());
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
        return voucherEsService.delete(indexName,id);
    }
}
