package com.smartLive.ai.strategy.milvus;

import com.smartLive.ai.utils.EsTool;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.interaction.api.DTO.ReviewDTO;
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
public class ReviewMilvusStrategy implements MilvusSyncStrategy<ReviewDTO> {

    @Autowired
    @Qualifier("reviewVectorStore")
    private VectorStore reviewVectorStore;

    @Override
    public Integer getType() {
        return GlobalBizTypeEnum.REVIEW.getCode();
    }

    @Override
    public boolean insertOrUpdate(String id, Object rawData) throws IOException {
        ReviewDTO doc = EsTool.convertToObject((Map) rawData, ReviewDTO.class);
        delete(id);
        if (doc == null) {
            return true;
        }
        Document document = createDocument(doc);
        if (document != null) {
            reviewVectorStore.add(List.of(document));
        }
        return true;
    }

    @Override
    public boolean batchInsert(List<Object> rawDataList) throws IOException {
        List<ReviewDTO> reviews = deduplicateById(EsTool.convertList(rawDataList, ReviewDTO.class));
        int batchSize = 10;
        for (int i = 0; i < reviews.size(); i += batchSize) {
            int end = Math.min(i + batchSize, reviews.size());
            List<ReviewDTO> batch = reviews.subList(i, end);
            deleteBatch(batch.stream().map(review -> String.valueOf(review.getId())).toList());
            List<Document> documents = batch.stream()
                    .map(this::createDocument)
                    .filter(Objects::nonNull)
                    .toList();
            if (!documents.isEmpty()) {
                reviewVectorStore.add(documents);
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
    public Document createDocument(ReviewDTO review) {
        if (review == null || review.getId() == null) {
            return null;
        }
        Map<String, Object> metadata = new HashMap<>();
        putIfNotNull(metadata, "id", review.getId());
        putIfNotNull(metadata, "userId", review.getUserId());
        putIfNotNull(metadata, "shopId", review.getShopId());
        putIfNotNull(metadata, "orderId", review.getOrderId());
        putIfNotNull(metadata, "sourceId", review.getSourceId());
        putIfNotNull(metadata, "sourceType", review.getSourceType());
        putIfNotNull(metadata, "status", review.getStatus());
        putIfNotNull(metadata, "isAIGenerated", review.getIsAIGenerated());
        putIfNotNull(metadata, "score", review.getScore());
        putIfNotNull(metadata, "serviceScore", review.getServiceScore());
        putIfNotNull(metadata, "tasteScore", review.getTasteScore());
        putIfNotNull(metadata, "envScore", review.getEnvScore());
        metadata.put("liked", review.getLiked() != null ? review.getLiked() : 0);
        metadata.put("replyCount", review.getReplyCount() != null ? review.getReplyCount() : 0);
        metadata.put("stared", review.getStared() != null ? review.getStared() : 0);
        putIfNotNull(metadata, "nickName", review.getNickName());
        putIfNotNull(metadata, "userIcon", review.getUserIcon());
        putIfNotNull(metadata, "images", review.getImages());
        metadata.put("createTime", review.getCreateTime() != null ? review.getCreateTime().getTime() : System.currentTimeMillis());
        metadata.put("isAnonymous", review.getIsAnonymous() != null ? review.getIsAnonymous() : false);
        putIfNotNull(metadata, "sourceName", review.getSourceName());

        return Document.builder()
                .id(String.valueOf(review.getId()))
                .text(buildContent(review))
                .metadata(metadata)
                .build();
    }

    private List<ReviewDTO> deduplicateById(List<ReviewDTO> rawList) {
        if (rawList == null || rawList.isEmpty()) {
            return List.of();
        }
        Map<String, ReviewDTO> unique = new LinkedHashMap<>();
        for (ReviewDTO review : rawList) {
            if (review != null && review.getId() != null) {
                unique.put(String.valueOf(review.getId()), review);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private void deleteBatch(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        reviewVectorStore.delete(ids);
        reviewVectorStore.delete(String.format("id in [%s]", String.join(", ", ids)));
    }

    private void deleteById(String id) {
        reviewVectorStore.delete(List.of(id));
        reviewVectorStore.delete(String.format("id == %s", id));
    }

    private String buildContent(ReviewDTO review) {
        if (review.getContent() != null && !review.getContent().isBlank()) {
            return review.getContent().trim();
        }
        if (review.getSourceName() != null && !review.getSourceName().isBlank()) {
            return review.getSourceName().trim();
        }
        return String.valueOf(review.getId());
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}