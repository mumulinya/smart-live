package com.smartLive.ai.strategy.milvus;

import com.smartLive.ai.entity.DOC.BlogDoc;
import com.smartLive.ai.utils.EsTool;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
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

@Component
public class BlogMilvusStrategy implements MilvusSyncStrategy<BlogDoc> {

    @Autowired
    @Qualifier("blogVectorStore")
    private VectorStore blogVectorStore;

    @Override
    public Integer getType() {
        return GlobalBizTypeEnum.BLOG.getCode();
    }

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

    @Override
    public boolean delete(String id) throws IOException {
        deleteById(id);
        return true;
    }

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

    private void deleteBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        blogVectorStore.delete(ids);
        blogVectorStore.delete(String.format("id in [%s]", String.join(", ", ids)));
    }

    private void deleteById(String id) {
        blogVectorStore.delete(List.of(id));
        blogVectorStore.delete(String.format("id == %s", id));
    }

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

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

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