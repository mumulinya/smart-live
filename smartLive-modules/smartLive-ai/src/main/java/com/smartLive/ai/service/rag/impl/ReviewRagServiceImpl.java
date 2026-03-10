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

import java.util.ArrayList;
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
    public String getReviewSummary(Integer sourceType, Long sourceId,
                                   Integer minScore, Integer maxScore, String userMessage) {
        // 1. 构建基础 filter
        String baseFilter = buildFilter(sourceType, sourceId);
        log.info("📝 评价 RAG baseFilter: {}", baseFilter);

        // 2. 用户指定了评分范围 → 只搜指定范围
        if (minScore != null || maxScore != null) {
            return searchByScoreRange(baseFilter, minScore, maxScore);
        }

        // 3. 用户没有指定评分 → 好中差全部搜索，综合总结
        return searchAllRange(baseFilter);
    }
    /**
     * 用户指定了评分范围，只搜指定范围
     */
    private String searchByScoreRange(String baseFilter, Integer minScore, Integer maxScore) {
        String scoreFilter = buildScoreFilter(minScore, maxScore);

        // 根据评分范围判断搜索关键词
        String query = (minScore != null && minScore >= 4)
                ? "好吃 满意 推荐 不错 服务好"
                : "差 失望 不推荐 难吃 服务差";

        log.info("📊 指定评分搜索 scoreFilter={}", scoreFilter);
        List<Document> reviews = searchReviews(query, baseFilter, scoreFilter, 15);

        if (reviews.isEmpty()) {
            return "暂无相关评价数据";
        }

        StringBuilder context = new StringBuilder();
        context.append("以下是筛选后的真实用户评价，请综合总结给用户，不要逐条列出原文：\n\n");
        reviews.forEach(d -> context.append("- ").append(d.getText()).append("\n"));
        return context.toString();
    }

    /**
     * 用户未指定评分，好中差全部搜索
     */
    private String searchAllRange(String baseFilter) {
        List<Document> goodReviews   = searchReviews("好吃 满意 推荐 不错 服务好", baseFilter, "score >= 4", 10);
        List<Document> normalReviews = searchReviews("一般 还行 普通 凑合",         baseFilter, "score == 3", 5);
        List<Document> badReviews    = searchReviews("差 失望 不推荐 难吃 服务差",   baseFilter, "score < 3",  5);

        log.info("📊 好评{}条 中评{}条 差评{}条",
                goodReviews.size(), normalReviews.size(), badReviews.size());

        if (goodReviews.isEmpty() && normalReviews.isEmpty() && badReviews.isEmpty()) {
            return "暂无评价数据";
        }

        StringBuilder context = new StringBuilder();
        context.append("以下是真实用户评价数据，请综合总结给用户，不要逐条列出原文，用自然语言描述整体口碑：\n\n");
        appendReviews(context, "好评", goodReviews);
        appendReviews(context, "中评", normalReviews);
        appendReviews(context, "差评", badReviews);
        return context.toString();
    }

    /**
     * 从向量库搜索评价
     */
    private List<Document> searchReviews(String query, String baseFilter,
                                         String scoreFilter, int topK) {
        try {
            String fullFilter = baseFilter.isEmpty()
                    ? scoreFilter
                    : baseFilter + " && " + scoreFilter;

            log.info("🔍 评价搜索 fullFilter={}", fullFilter);
            return reviewVectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(topK)
                            .filterExpression(fullFilter)
                            .build()
            );
        } catch (Exception e) {
            log.warn("⚠️ 评价搜索失败 scoreFilter={} error={}", scoreFilter, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 拼接评价内容到上下文
     */
    private void appendReviews(StringBuilder context, String label, List<Document> reviews) {
        if (reviews.isEmpty()) return;
        context.append(label).append("：\n");
        reviews.forEach(d -> context.append("- ").append(d.getText()).append("\n"));
        context.append("\n");
    }

    /**
     * 构建基础 Milvus filter
     */
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

    /**
     * 构建评分范围 filter
     */
    private String buildScoreFilter(Integer minScore, Integer maxScore) {
        if (minScore != null && maxScore != null) {
            return "score >= " + minScore + " && score <= " + maxScore;
        } else if (minScore != null) {
            return "score >= " + minScore;
        } else {
            return "score <= " + maxScore;
        }
    }
}