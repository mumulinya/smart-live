package com.smartLive.ai.strategy.milvus;

import com.smartLive.ai.entity.DOC.ShopDoc;
import com.smartLive.ai.utils.EsTool;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ShopMilvusStrategy implements MilvusSyncStrategy<ShopDoc> {

    @Autowired
    @Qualifier("shopVectorStore")
    private VectorStore shopVectorStore;

    @Override
    public Integer getType() {
        return GlobalBizTypeEnum.SHOP.getCode();
    }

    @Override
    public boolean insertOrUpdate(String id, Object rawData) throws IOException {
        ShopDoc doc = EsTool.convertToObject((Map) rawData, ShopDoc.class);
        delete(id);
        if (doc == null) {
            return true;
        }
        Document document = createDocument(doc);
        if (document != null) {
            shopVectorStore.add(List.of(document));
        }
        return true;
    }

    @Override
    public boolean batchInsert(List<Object> rawDataList) throws IOException {
        List<ShopDoc> shops = deduplicateById(EsTool.convertList(rawDataList, ShopDoc.class));
        int batchSize = 10;
        for (int i = 0; i < shops.size(); i += batchSize) {
            int end = Math.min(i + batchSize, shops.size());
            List<ShopDoc> batch = shops.subList(i, end);
            deleteBatch(batch.stream().map(shop -> String.valueOf(shop.getId())).toList());
            List<Document> documents = batch.stream()
                    .map(this::createDocument)
                    .filter(Objects::nonNull)
                    .toList();
            if (!documents.isEmpty()) {
                shopVectorStore.add(documents);
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
    public Document createDocument(ShopDoc shop) {
        if (shop == null || shop.getId() == null) {
            return null;
        }
        Map<String, Object> metadata = new HashMap<>();
        putIfNotNull(metadata, "id", shop.getId());
        putIfNotNull(metadata, "name", shop.getName());
        putIfNotNull(metadata, "typeId", shop.getTypeId());
        putIfNotNull(metadata, "images", shop.getImages());
        putIfNotNull(metadata, "shopLogo", shop.getShopLogo());
        putIfNotNull(metadata, "area", shop.getArea());
        putIfNotNull(metadata, "address", shop.getAddress());
        putIfNotNull(metadata, "x", shop.getX());
        putIfNotNull(metadata, "y", shop.getY());
        putIfNotNull(metadata, "avgPrice", shop.getAvgPrice());
        putIfNotNull(metadata, "sold", shop.getSold());
        putIfNotNull(metadata, "comments", shop.getComments());
        putIfNotNull(metadata, "score", shop.getScore());
        putIfNotNull(metadata, "openHours", shop.getOpenHours());

        return Document.builder()
                .id(String.valueOf(shop.getId()))
                .text(buildContent(shop.getName(), shop.getArea(), shop.getAddress()))
                .metadata(metadata)
                .build();
    }

    private List<ShopDoc> deduplicateById(List<ShopDoc> rawList) {
        if (rawList == null || rawList.isEmpty()) {
            return List.of();
        }
        Map<String, ShopDoc> unique = new LinkedHashMap<>();
        for (ShopDoc shop : rawList) {
            if (shop != null && shop.getId() != null) {
                unique.put(String.valueOf(shop.getId()), shop);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private void deleteBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        shopVectorStore.delete(ids);
        shopVectorStore.delete(String.format("id in [%s]", String.join(", ", ids)));
    }

    private void deleteById(String id) {
        shopVectorStore.delete(List.of(id));
        shopVectorStore.delete(String.format("id == %s", id));
    }

    private String buildContent(String... values) {
        List<String> segments = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                segments.add(value.trim());
            }
        }
        return segments.isEmpty() ? "shop" : String.join(" ", segments);
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}