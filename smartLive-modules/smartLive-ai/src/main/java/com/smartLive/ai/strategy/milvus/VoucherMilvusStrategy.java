package com.smartLive.ai.strategy.milvus;

import com.smartLive.ai.entity.DOC.VoucherDoc;
import com.smartLive.ai.utils.EsTool;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Component
public class VoucherMilvusStrategy implements MilvusSyncStrategy<VoucherDoc>{
    @Autowired
    @Qualifier("voucherVectorStore")
    private VectorStore voucherVectorStore;
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
        List<Document> existing = voucherVectorStore.similaritySearch(id);
        if (!existing.isEmpty()) {
            // 删除
            delete(id);
        }
        Document document = createDocument(doc);
        List<Document> list=new ArrayList<>();
        list.add(document);
        voucherVectorStore.add(list);
        return true;
    }

    @Override
    public boolean batchInsert(List<Object> rawDataList) throws IOException {
        List<VoucherDoc> list = EsTool.convertList(rawDataList, VoucherDoc.class);
        // 分批处理，每批10个
        int batchSize = 10;
        for (int i = 0; i < list.size(); i += batchSize) {
            int end = Math.min(i + batchSize, list.size());
            List<VoucherDoc> batch = list.subList(i, end);

            // 转换为Document并添加元数据
            List<Document> documents = batch.stream()
                    .map(this::createDocument)
                    .toList();
            System.out.println("写入第 " + (i / batchSize + 1) + " 批数据，数量: " + documents.size());
            voucherVectorStore.add(documents);
        }
        return true;
    }

    @Override
    public boolean delete(String id) throws IOException {
        // 1. 构建Spring AI的过滤条件（匹配元数据中的id）
        // 语法与你查询时的filterExpression一致：数值类型直接写，字符串用单引号
        String filterExpr = String.format("id == %s", id);
        // 2. 执行删除（Spring AI VectorStore的delete方法）
        // Spring AI 2.x的MilvusVectorStore.delete支持过滤表达式/String类型的filter
        voucherVectorStore.delete(filterExpr);

        return true;
    }
    @Override
    public Document createDocument(VoucherDoc voucher) {
        // 1. 文档内容：需要被搜索的文本
        String content = String.format("%s %s %s",
                voucher.getShopName(), voucher.getTitle(), voucher.getRules());

        // 构建元数据
        Map<String, Object> metadata = new HashMap<>();
        putIfNotNull(metadata, "id", voucher.getId());
        putIfNotNull(metadata, "shopId", voucher.getShopId());
        putIfNotNull(metadata, "typeId", voucher.getTypeId());
        putIfNotNull(metadata, "shopName", voucher.getShopName());
        putIfNotNull(metadata, "title", voucher.getTitle());
        putIfNotNull(metadata, "subTitle", voucher.getSubTitle());
        putIfNotNull(metadata, "rules", voucher.getRules());
        putIfNotNull(metadata, "payValue", voucher.getPayValue());
        putIfNotNull(metadata, "actualValue", voucher.getActualValue());
        putIfNotNull(metadata, "type", voucher.getType());
        putIfNotNull(metadata, "status", voucher.getStatus());
        putIfNotNull(metadata, "stock", voucher.getStock());
        putIfNotNull(metadata, "beginTime", voucher.getBeginTime());
        putIfNotNull(metadata, "endTime", voucher.getEndTime());
        return new Document(content, metadata);
    }
    // 工具方法防止输入空值
    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
