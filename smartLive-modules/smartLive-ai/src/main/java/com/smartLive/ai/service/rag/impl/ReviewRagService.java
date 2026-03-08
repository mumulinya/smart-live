package com.smartLive.ai.service.rag.impl;

import com.smartLive.ai.service.rag.IReviewRagService;
import com.smartLive.interaction.api.DTO.ReviewDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class ReviewRagService implements IReviewRagService {
    private final VectorStore reviewVectorStore;

    public ReviewRagService(@Qualifier("reviewVectorStore") VectorStore vectorStore) {
        this.reviewVectorStore = vectorStore;
    }

    /**
     * 获取评价列表
     *
     * @param reviewDTO   查询条件
     * @param userMessage 用户原始消息
     * @return 评价列表
     */
    @Override
    public List<ReviewDTO> getReviews(ReviewDTO reviewDTO, String userMessage) {
        // 使用用户原始消息作为RAG查询
        String ragQuery = userMessage != null ? userMessage : "评价";

        // 调用RAG - 使用用户消息
        List<Document> results = reviewVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(ragQuery)
                        .topK(5)
                        .filterExpression(buildFilterExpression(reviewDTO))
                        .build());
        log.info("🔍 评价RAG搜索结果：{}", results);
        if (results.isEmpty()) {
            return null;
        }
        List<ReviewDTO> reviewList = convertDocumentsToReviewDTO(results);
        log.info("🔍 RAG搜索到 {} 条评价", reviewList.size());
        return reviewList;
    }

    private String buildFilterExpression(ReviewDTO reviewDTO) {
        List<String> filters = new ArrayList<>();
        if (reviewDTO.getSourceType() != null) {
            filters.add("sourceType == " + reviewDTO.getSourceType());
        }
        if (reviewDTO.getSourceName() != null) {
            filters.add(String.format("sourceName == '%s'", reviewDTO.getSourceName()));
        }
        if (reviewDTO.getSourceId() != null) {
            filters.add("sourceId == " + reviewDTO.getSourceId());
        }
        return filters.isEmpty() ? "" : String.join(" && ", filters);
    }

    /**
     * 将Document列表转为ReviewDTO列表
     */
    private List<ReviewDTO> convertDocumentsToReviewDTO(List<Document> documents) {
        return documents.stream()
                .map(this::convertDocumentToReviewDTO)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 将单个Document转为ReviewDTO
     */
    private ReviewDTO convertDocumentToReviewDTO(Document document) {
        try {
            Map<String, Object> metadata = document.getMetadata();

            ReviewDTO review = new ReviewDTO();
            if (metadata.containsKey("id")) {
                review.setId(Long.valueOf(metadata.get("id").toString()));
            }
            if (metadata.containsKey("userId")) {
                review.setUserId(Long.valueOf(metadata.get("userId").toString()));
            }
            if (metadata.containsKey("shopId")) {
                review.setShopId(Long.valueOf(metadata.get("shopId").toString()));
            }
            if (metadata.containsKey("orderId")) {
                review.setOrderId(Long.valueOf(metadata.get("orderId").toString()));
            }
            if (metadata.containsKey("sourceType")) {
                review.setSourceType(Integer.valueOf(metadata.get("sourceType").toString()));
            }
            if (metadata.containsKey("sourceId")) {
                review.setSourceId(Long.valueOf(metadata.get("sourceId").toString()));
            }
            if (metadata.containsKey("sourceName")) {
                review.setSourceName(metadata.get("sourceName").toString());
            }
            if (metadata.containsKey("images")) {
                review.setImages(metadata.get("images").toString());
            }
            if (metadata.containsKey("content")) {
                review.setContent(metadata.get("content").toString());
            }
            if (metadata.containsKey("liked")) {
                review.setLiked(Integer.valueOf(metadata.get("liked").toString()));
            }
            if (metadata.containsKey("replyCount")) {
                review.setReplyCount(Integer.valueOf(metadata.get("replyCount").toString()));
            }
            if (metadata.containsKey("stared")) {
                review.setStared(Integer.valueOf(metadata.get("stared").toString()));
            }
            if (metadata.containsKey("status")) {
                review.setStatus(Integer.valueOf(metadata.get("status").toString()));
            }
            if (metadata.containsKey("score")) {
                review.setScore(Integer.valueOf(metadata.get("score").toString()));
            }
            if (metadata.containsKey("serviceScore")) {
                review.setServiceScore(Short.valueOf(metadata.get("serviceScore").toString()));
            }
            if (metadata.containsKey("tasteScore")) {
                review.setTasteScore(Short.valueOf(metadata.get("tasteScore").toString()));
            }
            if (metadata.containsKey("envScore")) {
                review.setEnvScore(Short.valueOf(metadata.get("envScore").toString()));
            }
            if (metadata.containsKey("nickName")) {
                review.setNickName(metadata.get("nickName").toString());
            }
            if (metadata.containsKey("userIcon")) {
                review.setUserIcon(metadata.get("userIcon").toString());
            }
            if (metadata.containsKey("createTime")) {
                Object createTimeObj = metadata.get("createTime");
                if (createTimeObj instanceof Long) {
                    Date createTime = new Date((Long) createTimeObj);
                    review.setCreateTime(createTime);
                }
            }

            return review;
        } catch (Exception e) {
            log.warn("转换Document到ReviewDTO失败: {}", e.getMessage());
            return null;
        }
    }

    public List<Document> getReviewDocuments(ReviewDTO reviewDTO) {
        List<Document> results = reviewVectorStore.similaritySearch(
                SearchRequest.builder()
                        .filterExpression(buildFilterExpression(reviewDTO))
                        .build());
        log.info("🔍 评价RAG搜索结果：{}", results);
        if (results.isEmpty()) {
            return null;
        }
        return results;
    }
}
