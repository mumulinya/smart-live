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
import java.util.List;
import java.util.Map;

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
            List<Document> list = new ArrayList<>();
            list.add(document);
            blogVectorStore.add(list);
        }
        return true;
    }

    @Override
    public boolean batchInsert(List<Object> rawDataList) throws IOException {
        List<BlogDoc> list = EsTool.convertList(rawDataList, BlogDoc.class);
        int batchSize = 10;
        for (int i = 0; i < list.size(); i += batchSize) {
            int end = Math.min(i + batchSize, list.size());
            List<BlogDoc> batch = list.subList(i, end);

            List<String> idList = batch.stream()
                    .map(blog -> String.valueOf(blog.getId()))
                    .toList();
            if (!idList.isEmpty()) {
                String inExpr = String.join(", ", idList);
                String filterExpr = String.format("id in [%s]", inExpr);
                blogVectorStore.delete(filterExpr);
            }

            List<Document> documents = batch.stream()
                    .map(this::createDocument)
                    .filter(document -> document != null)
                    .toList();
            if (!documents.isEmpty()) {
                blogVectorStore.add(documents);
            }
        }
        return true;
    }

    @Override
    public boolean delete(String id) throws IOException {
        String filterExpr = String.format("id == %s", id);
        blogVectorStore.delete(filterExpr);
        return true;
    }

    @Override
    public Document createDocument(BlogDoc blog) {
        String content = buildContent(blog);

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

        return new Document(content, metadata);
    }

    private String buildContent(BlogDoc blog) {
        List<String> segments = new ArrayList<>();
        if (StringUtils.hasText(blog.getTitle())) {
            segments.add(blog.getTitle());
        }
        if (StringUtils.hasText(blog.getContent())) {
            segments.add(blog.getContent());
        }
        if (StringUtils.hasText(blog.getName())) {
            segments.add(blog.getName());
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