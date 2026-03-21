package com.smartLive.ai.strategy.milvus;

import com.smartLive.ai.entity.DOC.ShopDoc;
import com.smartLive.ai.utils.EsTool;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import lombok.extern.slf4j.Slf4j;
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

/**
 * 店铺Milvus策略类。
 */
@Component
@Slf4j
public class ShopMilvusStrategy implements MilvusSyncStrategy<ShopDoc> {

    @Autowired
    @Qualifier("shopVectorStore")
    private VectorStore shopVectorStore;

    /**
     * 获取类型。
     */
    @Override
    public Integer getType() {
        return GlobalBizTypeEnum.SHOP.getCode();
    }

    /**
     * 获取结果。
     */
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

    /**
     * 获取结果。
     */
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

    /**
     * 删除结果。
     */
    @Override
    public boolean delete(String id) throws IOException {
        deleteById(id);
        return true;
    }

    /**
     * 创建文档。
     */
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

    /**
     * 获取店铺文档列表。
     */
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

    /**
     * 批量删除数据。
     */
    private void deleteBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        safeDeleteByIds(ids);
    }

    /**
     * 按ID删除数据。
     */
    private void deleteById(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        safeDeleteByIds(List.of(id));
    }

    /**
     * 安全删除，删除不支持或数据不存在时直接跳过。
     */
    private void safeDeleteByIds(List<String> ids) {
        try {
            shopVectorStore.delete(ids);
        } catch (UnsupportedOperationException ex) {
            log.warn("shopVectorStore does not support delete, skip ids={}", ids);
        } catch (Exception ex) {
            log.warn("shopVectorStore delete skipped, ids={}, reason={}", ids, ex.getMessage());
        }
    }

    /**
     * 构建内容。
     */
    private String buildContent(String... values) {
        List<String> segments = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                segments.add(value.trim());
            }
        }
        return segments.isEmpty() ? "shop" : String.join(" ", segments);
    }

    /**
     * 处理 putIfNotNull 逻辑。
     */
    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
