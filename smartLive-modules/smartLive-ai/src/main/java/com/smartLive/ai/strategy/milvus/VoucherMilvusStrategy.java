package com.smartLive.ai.strategy.milvus;

import com.smartLive.ai.entity.DOC.VoucherDoc;
import com.smartLive.ai.service.business.IVoucherMilvusService;
import com.smartLive.ai.strategy.milvus.MilvusSyncStrategy;
import com.smartLive.ai.utils.EsTool;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
@Component
public class VoucherMilvusStrategy implements MilvusSyncStrategy{
    @Autowired
    private IVoucherMilvusService voucherMilvusService;
    /**
     * 获取策略的类型
     */
    @Override
    public Integer getType() {
        return GlobalBizTypeEnum.VOUCHER.getCode();
    }

    @Override
    public boolean insertOrUpdate(String id, Object rawData) throws IOException {
        // 1. 策略自己知道要把 Map 转成什么实体类，Listener 不需要知道
        VoucherDoc doc = EsTool.convertToObject((Map) rawData, VoucherDoc.class);
        // 2. 调用业务 Service
        return voucherMilvusService.insertOrUpdate(id, doc);
    }

    @Override
    public boolean batchInsert(List<Object> rawDataList) throws IOException {
        List<VoucherDoc> list = EsTool.convertList(rawDataList, VoucherDoc.class);
        return voucherMilvusService.batchInsert(list);
    }

    @Override
    public boolean delete(String id) throws IOException {
        return voucherMilvusService.delete(id);
    }
}
