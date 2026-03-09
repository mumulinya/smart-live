package com.smartLive.ai.service.rag.impl;

import com.smartLive.ai.entity.vo.ReviewVO;
import com.smartLive.ai.service.rag.IReviewRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReviewRagServiceImpl implements IReviewRagService {

    private final VectorStore reviewVectorStore;
    @Autowired
    public ReviewRagServiceImpl(@Qualifier("reviewVectorStore") VectorStore vectorStore) {
        this.reviewVectorStore = vectorStore;
    }
    @Override
    public List<ReviewVO> getReviews(ReviewVO reviewVO, String userMessage) {
        String ragQuery = (userMessage == null || userMessage.isBlank())
                ? "review comment"
                : userMessage;

        String filter = buildFilterExpression(reviewVO);

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(ragQuery)
                .topK(10);

        if (StringUtils.hasText(filter)) {
            builder.filterExpression(filter);
        }

        log.info("RAG query: {}, filter: {}", ragQuery, filter);

        List<Document> documents = reviewVectorStore.similaritySearch(builder.build());
        log.info("Review RAG search results count: {}", documents.size());

        return documents.stream()
                .map(this::documentToReviewVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewVO> getReviewsByScore(ReviewVO reviewVO, String userMessage) {
        String ragQuery = (userMessage == null || userMessage.isBlank())
                ? "review comment"
                : userMessage;

        String filter = buildFilterExpressionWithScore(reviewVO);

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(ragQuery)
                .topK(10);

        if (StringUtils.hasText(filter)) {
            builder.filterExpression(filter);
        }

        log.info("RAG query by score: {}, filter: {}", ragQuery, filter);

        List<Document> documents = reviewVectorStore.similaritySearch(builder.build());

        return documents.stream()
                .map(this::documentToReviewVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewVO> getReviewsSortByPopularity(ReviewVO reviewVO, String userMessage) {
        String ragQuery = (userMessage == null || userMessage.isBlank())
                ? "review comment"
                : userMessage;

        String filter = buildFilterExpression(reviewVO);

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(ragQuery)
                .topK(20);

        if (StringUtils.hasText(filter)) {
            builder.filterExpression(filter);
        }

        log.info("RAG query by popularity: {}, filter: {}", ragQuery, filter);

        List<Document> documents = reviewVectorStore.similaritySearch(builder.build());

        // 按热度排序：liked + replyCount
        return documents.stream()
                .map(this::documentToReviewVO)
                .sorted((a, b) -> {
                    int scoreA = (a.getLiked() != null ? a.getLiked() : 0) +
                            (a.getReplyCount() != null ? a.getReplyCount() : 0);
                    int scoreB = (b.getLiked() != null ? b.getLiked() : 0) +
                            (b.getReplyCount() != null ? b.getReplyCount() : 0);
                    return scoreB - scoreA;
                })
                .limit(10)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewVO> searchReviews(ReviewVO reviewVO, String userMessage) {
        String ragQuery = (userMessage == null || userMessage.isBlank())
                ? "review"
                : userMessage;

        String filter = "metadata.status == 0";

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(ragQuery)
                .topK(reviewVO.getLimit() != null ? reviewVO.getLimit() : 10);

//        if (StringUtils.hasText(filter)) {
//            builder.filterExpression(filter);
//        }

        log.info("Global search reviews: {}, filter: {}", ragQuery, filter);

        List<Document> documents = reviewVectorStore.similaritySearch(builder.build());

        return documents.stream()
                .map(this::documentToReviewVO)
                .collect(Collectors.toList());
    }

    // ========== 辅助方法 ==========

    private String buildFilterExpression(ReviewVO reviewVO) {
        StringBuilder filter = new StringBuilder();
//        filter.append("metadata.status == 0");

        if (reviewVO.getSourceType() != null) {
            filter.append(" && metadata.sourceType == ").append(reviewVO.getSourceType());
        }

        if (reviewVO.getSourceId() != null) {
            filter.append(" && metadata.sourceId == ").append(reviewVO.getSourceId());
        }

        if (reviewVO.getIsAIGenerated() != null && !reviewVO.getIsAIGenerated()) {
            filter.append(" && metadata.isAIGenerated == false");
        }

        return filter.toString();
    }

    private String buildFilterExpressionWithScore(ReviewVO reviewVO) {
        String baseFilter = buildFilterExpression(reviewVO);

        if (reviewVO.getMinScore() != null) {
            baseFilter += " && metadata.score >= " + reviewVO.getMinScore();
        }

        return baseFilter;
    }

    private ReviewVO documentToReviewVO(Document document) {
        Map<String, Object> metadata = document.getMetadata();

        ReviewVO vo = new ReviewVO();
        vo.setId(Long.parseLong(metadata.get("id").toString()));
        vo.setUserId(Long.parseLong(metadata.get("userId").toString()));
        vo.setShopId(Long.parseLong(metadata.get("shopId").toString()));
        vo.setSourceId(Long.parseLong(metadata.get("sourceId").toString()));
        vo.setSourceType((Integer) metadata.get("sourceType"));

        vo.setContent((String) metadata.get("content"));
        vo.setScore((Integer) metadata.get("score"));

        Object serviceScoreObj = metadata.get("serviceScore");
        if (serviceScoreObj != null) {
            vo.setServiceScore(((Number) serviceScoreObj).shortValue());
        }

        Object tasteScoreObj = metadata.get("tasteScore");
        if (tasteScoreObj != null) {
            vo.setTasteScore(((Number) tasteScoreObj).shortValue());
        }

        Object envScoreObj = metadata.get("envScore");
        if (envScoreObj != null) {
            vo.setEnvScore(((Number) envScoreObj).shortValue());
        }

        vo.setLiked((Integer) metadata.get("liked"));
        vo.setReplyCount((Integer) metadata.get("replyCount"));
        vo.setStared((Integer) metadata.get("stared"));

        vo.setNickName((String) metadata.get("nickName"));
        vo.setUserIcon((String) metadata.get("userIcon"));
        vo.setSourceName((String) metadata.get("sourceName"));

        if (metadata.containsKey("relevance")) {
            vo.setRelevanceScore(Float.parseFloat(metadata.get("relevance").toString()));
        }

        return vo;
    }
}