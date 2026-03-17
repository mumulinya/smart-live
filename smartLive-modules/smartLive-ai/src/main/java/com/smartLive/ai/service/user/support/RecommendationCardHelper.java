package com.smartLive.ai.service.user.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
/**
 * 处理推荐卡片 JSON：
 * 1. 给前端渲染时做标准化
 * 2. 给 ChatMemory 恢复时提取成紧凑摘要，避免把整段 JSON 塞进模型上下文
 */
public class RecommendationCardHelper {

    private final ObjectMapper objectMapper;

    /**
     * 提取推荐 JSON。
     */
    public String extractRecommendationJson(String content) {
        if (!hasText(content)) {
            return null;
        }

        int end = content.lastIndexOf("}");
        if (end < 0) {
            return null;
        }

        int braceCount = 0;
        int start = -1;
        for (int i = end; i >= 0; i--) {
            char c = content.charAt(i);
            if (c == '}') {
                braceCount++;
            }
            else if (c == '{') {
                braceCount--;
                if (braceCount == 0) {
                    start = i;
                    break;
                }
            }
        }

        if (start < 0) {
            return null;
        }

        String json = content.substring(start, end + 1);
        return isRecommendationJson(json) ? json : null;
    }

    /**
     * 提取回复文本。
     */
    public String extractReplyText(String content) {
        String json = extractRecommendationJson(content);
        if (json == null) {
            return null;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            return normalizeText(jsonNode.path("replyText").asText(null));
        }
        catch (Exception e) {
            log.debug("Failed to extract replyText from recommendation JSON", e);
            return null;
        }
    }

    /**
     * 构建记忆摘要。
     */
    public String buildMemorySummary(String content) {
        String json = extractRecommendationJson(content);
        if (json == null) {
            return null;
        }

        try {
            JsonNode parsed = objectMapper.readTree(json);
            if (!(parsed instanceof ObjectNode root)) {
                return null;
            }

            // ChatMemory 中优先保留自然语言答复，再附上可被后续追问引用的实体锚点
            String replyText = normalizeText(root.path("replyText").asText(null));
            String recommendationSummary = buildRecommendationSummary(root);
            if (!hasText(replyText)) {
                return recommendationSummary;
            }
            if (!hasText(recommendationSummary)) {
                return "replyText=" + replyText;
            }
            return "replyText=" + replyText + "; " + recommendationSummary;
        }
        catch (Exception e) {
            log.debug("Failed to build memory summary from recommendation JSON", e);
            return extractReplyText(content);
        }
    }

    /**
     * 规范化推荐 JSON。
     */
    public String normalizeRecommendationJson(String json) {
        try {
            JsonNode parsed = objectMapper.readTree(json);
            if (!(parsed instanceof ObjectNode root)) {
                return json;
            }

            String type = resolveRecommendationType(root);
            if (!hasText(root.path("type").asText(null))) {
                root.put("type", type);
            }

            JsonNode recommendationsNode = root.get("recommendations");
            if (recommendationsNode instanceof ArrayNode recommendations) {
                for (JsonNode node : recommendations) {
                    if (!(node instanceof ObjectNode recommendation)) {
                        continue;
                    }
                    if (!hasText(recommendation.path("type").asText(null))) {
                        recommendation.put("type", type);
                    }
                }
            }

            return objectMapper.writeValueAsString(root);
        }
        catch (Exception e) {
            log.warn("Failed to normalize recommendation JSON, keep original", e);
            return json;
        }
    }

    /**
     * 构建推荐摘要。
     */
    private String buildRecommendationSummary(ObjectNode root) {
        JsonNode recommendationsNode = root.get("recommendations");
        if (!(recommendationsNode instanceof ArrayNode recommendations) || recommendations.isEmpty()) {
            return null;
        }

        // 根据字段特征判断是店铺推荐还是商品推荐，后续按不同字段模板压缩
        boolean productRecommendation = isProductRecommendation(root, recommendations);
        List<String> summaries = new ArrayList<>();
        for (JsonNode node : recommendations) {
            if (!(node instanceof ObjectNode recommendation)) {
                continue;
            }

            String summary = productRecommendation
                    /**
                     * 构建商品摘要。
                     */
                    ? buildProductSummary(recommendation)
                    : buildShopSummary(recommendation);
            if (hasText(summary)) {
                summaries.add(summary);
            }
        }

        if (summaries.isEmpty()) {
            return null;
        }

        return productRecommendation
                ? "products=[" + String.join(", ", summaries) + "]"
                : "shops=[" + String.join(", ", summaries) + "]";
    }

    /**
     * 判断是否商品推荐。
     */
    private boolean isProductRecommendation(ObjectNode root, ArrayNode recommendations) {
        String type = root.path("type").asText(null);
        if ("product".equalsIgnoreCase(type) || "voucher".equalsIgnoreCase(type)) {
            return true;
        }
        if ("shop".equalsIgnoreCase(type)) {
            return false;
        }

        for (JsonNode node : recommendations) {
            if (!(node instanceof ObjectNode recommendation)) {
                continue;
            }
            if (looksLikeProductRecommendation(recommendation)) {
                return true;
            }
            if (looksLikeShopRecommendation(recommendation)) {
                return false;
            }
        }

        return false;
    }

    /**
     * 构建店铺摘要。
     */
    private String buildShopSummary(ObjectNode recommendation) {
        List<String> fields = new ArrayList<>();
        // 只保留能支撑“刚才那家店/第一家店/评分/均价”这类追问的核心字段
        appendField(fields, "id", recommendation.get("id"));
        appendField(fields, "name", recommendation.get("name"));
        appendField(fields, "score", recommendation.get("score"));
        appendField(fields, "distance", recommendation.get("distanceText"));
        appendField(fields, "avgPrice", recommendation.get("avgPrice"));
        if (fields.isEmpty()) {
            return null;
        }
        return "shop[" + String.join(",", fields) + "]";
    }

    /**
     * 构建商品摘要。
     */
    private String buildProductSummary(ObjectNode recommendation) {
        List<String> fields = new ArrayList<>();
        // 商品摘要要同时保留商品本身和所属店铺，方便后续做跨轮指代
        appendField(fields, "id", recommendation.get("id"));
        appendField(fields, "name", recommendation.get("name"));
        appendField(fields, "shopId", recommendation.get("shopId"));
        appendField(fields, "shopName", recommendation.get("shopName"));
        appendField(fields, "category", recommendation.get("category"));
        appendField(fields, "price", recommendation.get("price"));
        appendField(fields, "originalPrice", recommendation.get("originalPrice"));
        if (fields.isEmpty()) {
            return null;
        }
        return "product[" + String.join(",", fields) + "]";
    }

    /**
     * 追加字段。
     */
    private void appendField(List<String> fields, String key, JsonNode valueNode) {
        String value = normalizeNodeValue(valueNode);
        if (hasText(value)) {
            fields.add(key + "=" + value);
        }
    }

    /**
     * 规范化节点值。
     */
    private String normalizeNodeValue(JsonNode valueNode) {
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        return normalizeText(valueNode.asText());
    }

    /**
     * 判断是否推荐 JSON。
     */
    private boolean isRecommendationJson(String content) {
        if (!content.startsWith("{") || !content.endsWith("}")) {
            return false;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(content);
            return jsonNode.has("recommendations") && jsonNode.has("replyText");
        }
        catch (Exception e) {
            return false;
        }
    }

    /**
     * 解析推荐类型。
     */
    private String resolveRecommendationType(ObjectNode root) {
        String rootType = root.path("type").asText(null);
        if (hasText(rootType)) {
            return rootType;
        }

        JsonNode recommendationsNode = root.get("recommendations");
        if (recommendationsNode instanceof ArrayNode recommendations) {
            for (JsonNode node : recommendations) {
                if (!(node instanceof ObjectNode recommendation)) {
                    continue;
                }

                String itemType = recommendation.path("type").asText(null);
                if (hasText(itemType)) {
                    return itemType;
                }

                if (looksLikeProductRecommendation(recommendation)) {
                    return "voucher";
                }
                if (looksLikeShopRecommendation(recommendation)) {
                    return "shop";
                }
            }
        }

        return "shop";
    }

    /**
     * 获取结果。
     */
    private boolean looksLikeProductRecommendation(ObjectNode recommendation) {
        return hasAnyField(recommendation,
                "shopId",
                "shopName",
                "category",
                "price",
                "originalPrice",
                "voucherType",
                "title",
                "rules",
                "payValue",
                "actualValue",
                "stock");
    }

    /**
     * 获取结果。
     */
    private boolean looksLikeShopRecommendation(ObjectNode recommendation) {
        return hasAnyField(recommendation,
                "score",
                "distanceText",
                "avgPrice",
                "openHours",
                "address",
                "x",
                "y",
                "sold");
    }

    /**
     * 判断是否存在任意字段。
     */
    private boolean hasAnyField(ObjectNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isTextual()) {
                if (hasText(value.asText())) {
                    return true;
                }
                continue;
            }
            return true;
        }
        return false;
    }

    /**
     * 规范化文本。
     */
    private String normalizeText(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.replace("\r", "\n")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 判断文本是否存在。
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
