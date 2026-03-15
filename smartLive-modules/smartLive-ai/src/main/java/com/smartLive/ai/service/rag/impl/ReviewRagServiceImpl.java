package com.smartLive.ai.service.rag.impl;

import com.smartLive.ai.entity.vo.ReviewVO;
import com.smartLive.ai.service.rag.IReviewRagService;
import com.smartLive.ai.utils.RagMetadataValueUtils;
import com.smartLive.interaction.api.DTO.ReviewDTO;
import com.smartLive.interaction.api.RemoteReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReviewRagServiceImpl implements IReviewRagService {

    private final VectorStore reviewVectorStore;
    private final RemoteReviewService remoteReviewService;

    @Autowired
    public ReviewRagServiceImpl(@Qualifier("reviewVectorStore") VectorStore vectorStore,
                                RemoteReviewService remoteReviewService) {
        this.reviewVectorStore = vectorStore;
        this.remoteReviewService = remoteReviewService;
    }

    @Override
    public String getReviewSummary(Integer sourceType, Long sourceId,
                                   Integer minScore, Integer maxScore, String userMessage) {
        String baseFilter = buildSourceFilter(sourceType, sourceId);
        String scoreFilter = buildScoreFilter(minScore, maxScore);
        String query = StringUtils.hasText(userMessage) ? userMessage : "review summary";
        List<Document> docs = searchReviewDocs(query, baseFilter, scoreFilter, 15);
        if (docs.isEmpty()) {
            return "No review data.";
        }
        StringBuilder context = new StringBuilder("Review summary reference:\n");
        docs.forEach(doc -> context.append("- ").append(doc.getText()).append('\n'));
        return context.toString();
    }

    @Override
    public List<ReviewVO> searchReviews(String query, Long shopId) {
        if (shopId == null) {
            return List.of();
        }
        String safeQuery = StringUtils.hasText(query) ? query : "shop reviews";
        List<Document> docs = searchReviewDocs(safeQuery, buildShopFilter(shopId), null, 15);
        return convertDocumentsToReviewVo(docs);
    }

    @Override
    public List<ReviewVO> getReviewsByScore(Long shopId, Integer minScore, Integer maxScore) {
        if (shopId == null) {
            return List.of();
        }
        List<Document> docs = searchReviewDocs("low score reviews", buildShopFilter(shopId), buildScoreFilter(minScore, maxScore), 20);
        return convertDocumentsToReviewVo(docs);
    }

    @Override
    public ReviewVO getReviewById(Long reviewId, Long shopId) {
        if (reviewId == null) {
            return null;
        }
        ReviewDTO reviewDTO = remoteReviewService.getReviewById(reviewId);
        if (reviewDTO == null) {
            return null;
        }
        if (shopId != null && !shopId.equals(reviewDTO.getShopId())) {
            return null;
        }
        return convertReviewDtoToReviewVo(reviewDTO);
    }

    private List<Document> searchReviewDocs(String query, String baseFilter, String scoreFilter, int topK) {
        try {
            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(query)
                    .topK(topK);
            String fullFilter = mergeFilter(baseFilter, scoreFilter);
            if (StringUtils.hasText(fullFilter)) {
                builder.filterExpression(fullFilter);
            }
            List<Document> documents = reviewVectorStore.similaritySearch(builder.build());
            return documents == null ? new ArrayList<>() : documents;
        } catch (Exception ex) {
            log.warn("Review vector search failed, query={}, error={}", query, ex.getMessage());
            return new ArrayList<>();
        }
    }

    private String mergeFilter(String baseFilter, String extraFilter) {
        if (!StringUtils.hasText(baseFilter)) {
            return extraFilter;
        }
        if (!StringUtils.hasText(extraFilter)) {
            return baseFilter;
        }
        return baseFilter + " && " + extraFilter;
    }

    private String buildSourceFilter(Integer sourceType, Long sourceId) {
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
        return shopId == null ? "" : "shopId == " + shopId;
    }

    private String buildScoreFilter(Integer minScore, Integer maxScore) {
        if (minScore != null && maxScore != null) {
            return "score >= " + minScore + " && score <= " + maxScore;
        }
        if (minScore != null) {
            return "score >= " + minScore;
        }
        if (maxScore != null) {
            return "score <= " + maxScore;
        }
        return "";
    }

    private ReviewVO convertReviewDtoToReviewVo(ReviewDTO reviewDTO) {
        ReviewVO reviewVO = new ReviewVO();
        reviewVO.setId(reviewDTO.getId());
        reviewVO.setUserId(reviewDTO.getUserId());
        reviewVO.setShopId(reviewDTO.getShopId());
        reviewVO.setOrderId(reviewDTO.getOrderId());
        reviewVO.setSourceType(reviewDTO.getSourceType());
        reviewVO.setSourceName(reviewDTO.getSourceName());
        reviewVO.setSourceId(reviewDTO.getSourceId());
        reviewVO.setContent(reviewDTO.getContent());
        reviewVO.setImages(splitImages(reviewDTO.getImages()));
        reviewVO.setLiked(reviewDTO.getLiked());
        reviewVO.setReplyCount(reviewDTO.getReplyCount());
        reviewVO.setStared(reviewDTO.getStared());
        reviewVO.setStatus(reviewDTO.getStatus());
        reviewVO.setScore(reviewDTO.getScore());
        reviewVO.setServiceScore(reviewDTO.getServiceScore());
        reviewVO.setTasteScore(reviewDTO.getTasteScore());
        reviewVO.setEnvScore(reviewDTO.getEnvScore());
        reviewVO.setIsAnonymous(reviewDTO.getIsAnonymous());
        reviewVO.setIsAIGenerated(reviewDTO.getIsAIGenerated());
        reviewVO.setNickName(reviewDTO.getNickName());
        reviewVO.setUserIcon(reviewDTO.getUserIcon());
        reviewVO.setCreateTime(reviewDTO.getCreateTime());
        reviewVO.setIsLike(reviewDTO.getIsLike());
        reviewVO.setIsStared(reviewDTO.getIsStared());
        return reviewVO;
    }

    private List<String> splitImages(String images) {
        if (!StringUtils.hasText(images)) {
            return null;
        }
        return Arrays.stream(images.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
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
            reviewVo.setOrderId(RagMetadataValueUtils.toLong(metadata.get("orderId")));
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
        } catch (Exception ex) {
            log.warn("Convert review vector document failed: {}", ex.getMessage());
            return null;
        }
    }

    private Short toShort(Object value) {
        Integer number = RagMetadataValueUtils.toInteger(value);
        return number == null ? null : number.shortValue();
    }
}