package com.smartLive.ai.strategy.milvus;

import com.smartLive.ai.entity.DOC.ProductDoc;
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
public class ProductMilvusStrategy implements MilvusSyncStrategy<ProductDoc>{
    @Autowired
    @Qualifier("productVectorStore")
    private VectorStore productVectorStore;
    /**
     * 获取策略的类型
     */
    @Override
    public Integer getType() {
        return GlobalBizTypeEnum.PRODUCT.getCode(); // Keep PRODUCT enum code for now
    }

    @Override
    public boolean insertOrUpdate(String id, Object rawData) throws IOException {
        // 1. 策略自己知道要把 Map 转成什么实体类，Listener 不需要知道
        ProductDoc doc = EsTool.convertToObject((Map) rawData, ProductDoc.class);
        // 删除旧数据 (精准匹配 id 元数据)
        delete(id);
        
        Document document = createDocument(doc);
        if (document != null) {
            List<Document> list = new ArrayList<>();
            list.add(document);
            productVectorStore.add(list);
        }
        return true;
    }

    @Override
    public boolean batchInsert(List<Object> rawDataList) throws IOException {
        List<ProductDoc> list = EsTool.convertList(rawDataList, ProductDoc.class);
        // 分批处理，每批10个
        int batchSize = 10;
        for (int i = 0; i < list.size(); i += batchSize) {
            int end = Math.min(i + batchSize, list.size());
            List<ProductDoc> batch = list.subList(i, end);

            // 提取这一批的所有 id，用 in 表达式提前删除旧数据
            List<String> idList = batch.stream()
                    .map(product -> String.valueOf(product.getId()))
                    .toList();
            if (!idList.isEmpty()) {
                String inExpr = String.join(", ", idList);
                String filterExpr = String.format("id in [%s]", inExpr);
                productVectorStore.delete(filterExpr);
            }

            // 转换为Document并添加元数据
            List<Document> documents = batch.stream()
                    .map(this::createDocument)
                    .toList();
            System.out.println("写入第 " + (i / batchSize + 1) + " 批数据，数量: " + documents.size());
            productVectorStore.add(documents);
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
        productVectorStore.delete(filterExpr);

        return true;
    }
    @Override
    public Document createDocument(ProductDoc product) {
        // 1. 文档内容：需要被搜索的文本
        String content = String.format("%s %s %s",
                product.getShopName(), product.getName(), product.getRulesJson());

        // 构建元数据
        Map<String, Object> metadata = new HashMap<>();
        putIfNotNull(metadata, "id", product.getId());
        if (product.getShopId() != null && !product.getShopId().isEmpty()) {
            putIfNotNull(metadata, "shopId", product.getShopId());
        }
        putIfNotNull(metadata, "typeId", product.getTypeId());
        putIfNotNull(metadata, "shopName", product.getShopName());
        putIfNotNull(metadata, "name", product.getName());
        putIfNotNull(metadata, "subTitle", product.getSubTitle());
        putIfNotNull(metadata, "rulesJson", product.getRulesJson());
        putIfNotNull(metadata, "price", product.getPrice());
        putIfNotNull(metadata, "originalPrice", product.getOriginalPrice());
        putIfNotNull(metadata, "activityType", product.getActivityType());
        putIfNotNull(metadata, "status", product.getStatus());
        putIfNotNull(metadata, "stock", product.getStock());
        putIfNotNull(metadata, "category", product.getCategory());


        // Time and new fields
        putIfNotNull(metadata, "beginTime", product.getBeginTime());
        putIfNotNull(metadata, "endTime", product.getEndTime());
        putIfNotNull(metadata, "validityType", product.getValidityType());
        putIfNotNull(metadata, "validDays", product.getValidDays());
        putIfNotNull(metadata, "useStartTime", product.getUseStartTime());
        putIfNotNull(metadata, "useEndTime", product.getUseEndTime());

        return new Document(content, metadata);
    }
    // 工具方法防止输入空值
    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
