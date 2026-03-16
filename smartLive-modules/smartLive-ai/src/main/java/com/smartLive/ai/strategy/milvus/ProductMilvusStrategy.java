package com.smartLive.ai.strategy.milvus;

import com.smartLive.ai.entity.DOC.ProductDoc;
import com.smartLive.ai.utils.EsTool;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ProductMilvusStrategy implements MilvusSyncStrategy<ProductDoc> {

    @Autowired
    @Qualifier("productVectorStore")
    private VectorStore productVectorStore;

    @Override
    public Integer getType() {
        return GlobalBizTypeEnum.PRODUCT.getCode();
    }

    @Override
    public boolean insertOrUpdate(String id, Object rawData) throws IOException {
        ProductDoc doc = EsTool.convertToObject((Map) rawData, ProductDoc.class);
        delete(id);
        if (doc == null) {
            return true;
        }
        Document document = createDocument(doc);
        if (document != null) {
            productVectorStore.add(List.of(document));
        }
        return true;
    }

    @Override
    public boolean batchInsert(List<Object> rawDataList) throws IOException {
        List<ProductDoc> products = deduplicateById(EsTool.convertList(rawDataList, ProductDoc.class));
        int batchSize = 10;
        for (int i = 0; i < products.size(); i += batchSize) {
            int end = Math.min(i + batchSize, products.size());
            List<ProductDoc> batch = products.subList(i, end);
            deleteBatch(batch.stream().map(product -> String.valueOf(product.getId())).toList());
            List<Document> documents = batch.stream()
                    .map(this::createDocument)
                    .filter(Objects::nonNull)
                    .toList();
            if (!documents.isEmpty()) {
                productVectorStore.add(documents);
            }
        }
        return true;
    }

    @Override
    public boolean delete(String id) throws IOException {
        deleteById(id);
        return true;
    }

    @Override
    public Document createDocument(ProductDoc product) {
        if (product == null || product.getId() == null) {
            return null;
        }
        Map<String, Object> metadata = new HashMap<>();
        putIfNotNull(metadata, "id", product.getId());
        putIfNotNull(metadata, "shopId", product.getShopId());
        putIfNotNull(metadata, "name", product.getName());
        putIfNotNull(metadata, "subTitle", product.getSubTitle());
        putIfNotNull(metadata, "rulesJson", product.getRulesJson());
        putIfNotNull(metadata, "price", product.getPrice());
        putIfNotNull(metadata, "originalPrice", product.getOriginalPrice());
        putIfNotNull(metadata, "activityType", product.getActivityType());
        putIfNotNull(metadata, "status", product.getStatus());
        putIfNotNull(metadata, "stock", product.getStock());
        putIfNotNull(metadata, "category", product.getCategory());
        putIfNotNull(metadata, "coverImg", product.getCoverImg());
        putIfNotNull(metadata, "validityType", product.getValidityType());
        putIfNotNull(metadata, "validDays", product.getValidDays());
        putIfNotNull(metadata, "beginTime", formatDate(product.getBeginTime()));
        putIfNotNull(metadata, "endTime", formatDate(product.getEndTime()));
        putIfNotNull(metadata, "useStartTime", formatDate(product.getUseStartTime()));
        putIfNotNull(metadata, "useEndTime", formatDate(product.getUseEndTime()));

        return Document.builder()
                .id(String.valueOf(product.getId()))
                .text(buildContent(product.getName(), product.getSubTitle(), product.getRulesJson()))
                .metadata(metadata)
                .build();
    }

    private List<ProductDoc> deduplicateById(List<ProductDoc> rawList) {
        if (rawList == null || rawList.isEmpty()) {
            return List.of();
        }
        Map<String, ProductDoc> unique = new LinkedHashMap<>();
        for (ProductDoc product : rawList) {
            if (product != null && product.getId() != null) {
                unique.put(String.valueOf(product.getId()), product);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private void deleteBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        productVectorStore.delete(ids);
        productVectorStore.delete(String.format("id in [%s]", String.join(", ", ids)));
    }

    private void deleteById(String id) {
        productVectorStore.delete(List.of(id));
        productVectorStore.delete(String.format("id == %s", id));
    }

    private String buildContent(String... values) {
        List<String> segments = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                segments.add(value.trim());
            }
        }
        return segments.isEmpty() ? "product" : String.join(" ", segments);
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }
}