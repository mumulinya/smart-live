package com.smartLive.ai.strategy.milvus;

import com.smartLive.ai.entity.DOC.BlogDoc;
import com.smartLive.ai.utils.EsTool;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 博客Milvus策略类。
 */
@Component
@Slf4j
public class BlogMilvusStrategy implements MilvusSyncStrategy<BlogDoc> {

    @Autowired
    @Qualifier("blogVectorStore")
    private VectorStore blogVectorStore;

    /**
     * 获取类型。
     */
    @Override
    public Integer getType() {
        return GlobalBizTypeEnum.BLOG.getCode();
    }

    /**
     * 获取结果。
     */
    @Override
    public boolean insertOrUpdate(String id, Object rawData) throws IOException {
        BlogDoc doc = convertRawData(rawData);
        delete(id);
        if (doc == null) {
            return true;
        }
        Document document = createDocument(doc);
        if (document != null) {
            blogVectorStore.add(List.of(document));
        }
        return true;
    }

    /**
     * 获取结果。
     */
    @Override
    public boolean batchInsert(List<Object> rawDataList) throws IOException {
        List<BlogDoc> blogs = deduplicateById(EsTool.convertList(rawDataList, BlogDoc.class));
        int batchSize = 10;
        for (int i = 0; i < blogs.size(); i += batchSize) {
            int end = Math.min(i + batchSize, blogs.size());
            List<BlogDoc> batch = blogs.subList(i, end);
            deleteBatch(batch.stream().map(blog -> String.valueOf(blog.getId())).toList());
            List<Document> documents = batch.stream()
                    .map(this::createDocument)
                    .filter(Objects::nonNull)
                    .toList();
            if (!documents.isEmpty()) {
                blogVectorStore.add(documents);
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
    public Document createDocument(BlogDoc blog) {
        if (blog == null || blog.getId() == null) {
            return null;
        }
        Map<String, Object> metadata = new HashMap<>();
        putIfNotNull(metadata, "id", blog.getId());
        putIfNotNull(metadata, "shopId", blog.getShopId());
        putIfNotNull(metadata, "typeId", blog.getTypeId());
        putIfNotNull(metadata, "userId", blog.getUserId());
        putIfNotNull(metadata, "title", blog.getTitle());
        putIfNotNull(metadata, "images", blog.getImages());
        putIfNotNull(metadata, "content", blog.getContent());
        putIfNotNull(metadata, "liked", blog.getLiked());
        putIfNotNull(metadata, "comments", blog.getComments());
        putIfNotNull(metadata, "icon", blog.getIcon());
        putIfNotNull(metadata, "name", blog.getName());
        if (blog.getCreateTime() != null) {
            metadata.put("createTime", blog.getCreateTime().getTime());
        }

        return Document.builder()
                .id(String.valueOf(blog.getId()))
                .text(buildContent(blog))
                .metadata(metadata)
                .build();
    }

    /**
     * 获取博客文档列表。
     */
    private List<BlogDoc> deduplicateById(List<BlogDoc> rawList) {
        if (rawList == null || rawList.isEmpty()) {
            return List.of();
        }
        Map<String, BlogDoc> unique = new LinkedHashMap<>();
        for (BlogDoc blog : rawList) {
            if (blog != null && blog.getId() != null) {
                unique.put(String.valueOf(blog.getId()), blog);
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
            blogVectorStore.delete(ids);
        } catch (UnsupportedOperationException ex) {
            log.warn("blogVectorStore does not support delete, skip ids={}", ids);
        } catch (Exception ex) {
            log.warn("blogVectorStore delete skipped, ids={}, reason={}", ids, ex.getMessage());
        }
    }

    /**
     * 构建内容。
     */
    private String buildContent(BlogDoc blog) {
        List<String> segments = new ArrayList<>();
        if (StringUtils.hasText(blog.getTitle())) {
            segments.add(blog.getTitle().trim());
        }
        if (StringUtils.hasText(blog.getContent())) {
            segments.add(blog.getContent().trim());
        }
        if (StringUtils.hasText(blog.getName())) {
            segments.add(blog.getName().trim());
        }
        if (segments.isEmpty()) {
            return String.valueOf(blog.getId());
        }
        return String.join(" ", segments);
    }

    /**
     * 处理 putIfNotNull 逻辑。
     */
    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    /**
     * 转换rawdata。
     */
    private BlogDoc convertRawData(Object rawData) {
        if (rawData instanceof BlogDoc blogDoc) {
            return blogDoc;
        }
        if (!(rawData instanceof Map<?, ?> rawMap)) {
            return null;
        }
        Map<String, Object> data = new HashMap<>();
        rawMap.forEach((key, value) -> data.put(String.valueOf(key), value));
        return EsTool.convertToObject(data, BlogDoc.class);
    }
}
