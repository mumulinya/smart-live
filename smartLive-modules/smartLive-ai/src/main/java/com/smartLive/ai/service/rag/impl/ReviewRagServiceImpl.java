package com.smartLive.ai.service.rag.impl;

import com.smartLive.ai.entity.vo.ReviewVO;
import com.smartLive.ai.service.rag.IReviewRagService;
import com.smartLive.ai.utils.RagMetadataValueUtils;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReviewRagServiceImpl implements IReviewRagService {

    private final VectorStore reviewVectorStore;

    @Autowired
    public ReviewRagServiceImpl(@Qualifier("reviewVectorStore") VectorStore vectorStore) {
        this.reviewVectorStore = vectorStore;
    }

    @Override
    public String getReviewSummary(Integer sourceType, Long sourceId,
                                   Integer minScore, Integer maxScore, String userMessage) {
        String baseFilter = buildFilter(sourceType, sourceId);
        log.info("Review RAG baseFilter: {}", baseFilter);

        if (minScore != null || maxScore != null) {
            return searchByScoreRange(baseFilter, minScore, maxScore);
        }

        return searchAllRange(baseFilter);
    }

    @Override
    public List<ReviewVO> searchReviews(String query, Long shopId) {
        if (shopId == null) {
            return List.of();
        }
        String safeQuery = StringUtils.hasText(query) ? query : "店铺评价";
        String baseFilter = buildShopFilter(shopId);
        List<Document> docs = searchReviewDocs(safeQuery, baseFilter, "score >= 1 && score <= 5", 15);
        return convertDocumentsToReviewVo(docs);
    }

    @Override
    public List<ReviewVO> getReviewsByScore(Long shopId, Integer minScore, Integer maxScore) {
        if (shopId == null) {
            return List.of();
        }
        String baseFilter = buildShopFilter(shopId);
        String scoreFilter = buildScoreFilter(minScore, maxScore);
        if (!StringUtils.hasText(scoreFilter)) {
            scoreFilter = "score >= 1 && score <= 5";
        }
        List<Document> docs = searchReviewDocs("店铺 差评 服务 口味", baseFilter, scoreFilter, 20);
        return convertDocumentsToReviewVo(docs);
    }

    private String searchByScoreRange(String baseFilter, Integer minScore, Integer maxScore) {
        String scoreFilter = buildScoreFilter(minScore, maxScore);

        String query = (minScore != null && minScore >= 4)
                ? "好吃 满意 推荐 不错 服务好"
                : "差 失望 不推荐 难吃 服务差";

        log.info("Score range search, scoreFilter={}", scoreFilter);
        List<Document> reviews = searchReviewDocs(query, baseFilter, scoreFilter, 15);

        if (reviews.isEmpty()) {
            return "暂无相关评价数据";
        }

        StringBuilder context = new StringBuilder();
        context.append("以下是真实用户评价，请综合总结给用户，不要逐条复述原文：\n\n");
        reviews.forEach(d -> context.append("- ").append(d.getText()).append("\n"));
        return context.toString();
    }

    private String searchAllRange(String baseFilter) {
        List<Document> goodReviews = searchReviewDocs("好吃 满意 推荐 不错 服务好", baseFilter, "score >= 4", 10);
        List<Document> normalReviews = searchReviewDocs("一般 还行 普通 凑合", baseFilter, "score == 3", 5);
        List<Document> badReviews = searchReviewDocs("差 失望 不推荐 难吃 服务差", baseFilter, "score < 3", 5);

        log.info("Review summary counts: good={}, normal={}, bad={}",
                goodReviews.size(), normalReviews.size(), badReviews.size());

        if (goodReviews.isEmpty() && normalReviews.isEmpty() && badReviews.isEmpty()) {
            return "暂无评价数据";
        }

        StringBuilder context = new StringBuilder();
        context.append("以下是真实用户评价数据，请综合总结给用户，不要逐条复述原文：\n\n");
        appendReviews(context, "好评", goodReviews);
        appendReviews(context, "中评", normalReviews);
        appendReviews(context, "差评", badReviews);
        return context.toString();
    }

    private List<Document> searchReviewDocs(String query, String baseFilter,
                                            String scoreFilter, int topK) {
        try {
            String fullFilter = StringUtils.hasText(baseFilter)
                    ? StringUtils.hasText(scoreFilter) ? baseFilter + " && " + scoreFilter : baseFilter
                    : scoreFilter;

            log.info("Review search fullFilter={}", fullFilter);
            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(query)
                    .topK(topK);
            if (StringUtils.hasText(fullFilter)) {
                builder.filterExpression(fullFilter);
            }
            List<Document> documents = reviewVectorStore.similaritySearch(builder.build());
            return documents == null ? new ArrayList<>() : documents;
        } catch (Exception e) {
            log.warn("Review search failed, scoreFilter={}, error={}", scoreFilter, e.getMessage());
            return new ArrayList<>();
        }
    }

    private void appendReviews(StringBuilder context, String label, List<Document> reviews) {
        if (reviews.isEmpty()) {
            return;
        }
        context.append(label).append("：\n");
        reviews.forEach(d -> context.append("- ").append(d.getText()).append("\n"));
        context.append("\n");
    }

    private String buildFilter(Integer sourceType, Long sourceId) {
        List<String> filters = new ArrayList<>();
        if (sourceType != null) {
            filters.add("sourceType == " + sourceType);
        }
        if (sourceId != null) {
            filters.add("sourceId == " + sourceId);
        }
        return String.join(" && ", filters);
    }

    private String buildShopFilter(Long shopId) {
        return "sourceType == " + GlobalBizTypeEnum.SHOP.getCode() + " && shopId == " + shopId;
    }

    private String buildScoreFilter(Integer minScore, Integer maxScore) {
        if (minScore != null && maxScore != null) {
            return "score >= " + minScore + " && score <= " + maxScore;
        } else if (minScore != null) {
            return "score >= " + minScore;
        } else if (maxScore != null) {
            return "score <= " + maxScore;
        }
        return "";
    }

    private List<ReviewVO> convertDocumentsToReviewVo(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        List<ReviewVO> list = new ArrayList<>(documents.size());
        for (Document document : documents) {
            ReviewVO reviewVo = convertDocumentToReviewVo(document);
            if (reviewVo != null) {
                list.add(reviewVo);
            }
        }
        return list;
    }

    private ReviewVO convertDocumentToReviewVo(Document document) {
        if (document == null) {
            return null;
        }
        try {
            Map<String, Object> metadata = document.getMetadata();
            ReviewVO reviewVo = new ReviewVO();
            reviewVo.setId(RagMetadataValueUtils.toLong(metadata.get("id")));
            reviewVo.setUserId(RagMetadataValueUtils.toLong(metadata.get("userId")));
            reviewVo.setShopId(RagMetadataValueUtils.toLong(metadata.get("shopId")));
            reviewVo.setSourceId(RagMetadataValueUtils.toLong(metadata.get("sourceId")));
            reviewVo.setSourceType(RagMetadataValueUtils.toInteger(metadata.get("sourceType")));
            reviewVo.setScore(RagMetadataValueUtils.toInteger(metadata.get("score")));
            reviewVo.setServiceScore(toShort(metadata.get("serviceScore")));
            reviewVo.setTasteScore(toShort(metadata.get("tasteScore")));
            reviewVo.setEnvScore(toShort(metadata.get("envScore")));
            reviewVo.setLiked(RagMetadataValueUtils.toInteger(metadata.get("liked")));
            reviewVo.setReplyCount(RagMetadataValueUtils.toInteger(metadata.get("replyCount")));
            reviewVo.setStared(RagMetadataValueUtils.toInteger(metadata.get("stared")));
            reviewVo.setNickName(RagMetadataValueUtils.toStringValue(metadata.get("nickName")));
            reviewVo.setUserIcon(RagMetadataValueUtils.toStringValue(metadata.get("userIcon")));
            reviewVo.setSourceName(RagMetadataValueUtils.toStringValue(metadata.get("sourceName")));
            reviewVo.setContent(document.getText());

            Long createTime = RagMetadataValueUtils.toLong(metadata.get("createTime"));
            if (createTime != null) {
                reviewVo.setCreateTime(new Date(createTime));
            }
            return reviewVo;
        } catch (Exception e) {
            log.warn("Convert document to ReviewVO failed: {}", e.getMessage());
            return null;
        }
    }

    private Short toShort(Object value) {
        Integer number = RagMetadataValueUtils.toInteger(value);
        return number == null ? null : number.shortValue();
    }
}
