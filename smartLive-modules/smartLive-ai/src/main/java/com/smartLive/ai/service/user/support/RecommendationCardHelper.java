package com.smartLive.ai.service.user.support;

import com.smartLive.ai.domain.DTO.BaseStructuredResponseDTO;
import com.smartLive.ai.domain.DTO.OrderCardResponseDTO;
import com.smartLive.ai.domain.DTO.OrderFailCardResponseDTO;
import com.smartLive.ai.domain.DTO.ProductCardResponseDTO;
import com.smartLive.ai.domain.DTO.ShopCardResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
/**
 * 用户侧结构化响应处理器：
 * 1. 面向前端渲染做字段标准化。
 * 2. 面向会话记忆恢复提取紧凑摘要，避免整段 JSON 进入模型上下文。
 */
public class RecommendationCardHelper {

    private static final String MARKDOWN_FENCE = "```";
    private static final String MARKDOWN_JSON_FENCE = "```json";

    private final ObjectMapper objectMapper;

    /**
     * 提取结构化 JSON。
     */
    public String extractRecommendationJson(String content) {
        if (!hasText(content)) {
            return null;
        }

        String json = extractLastJsonObject(content);
        if (json == null) {
            return null;
        }
        StructuredResponseParseResult parseResult = parseStructuredResponse(json);
        return parseResult != null ? parseResult.normalizedJson : null;
    }

    /**
     * 描述结构化响应校验失败原因。
     */
    public String describeStructuredValidationFailure(String content) {
        if (!hasText(content)) {
            return "模型没有返回任何内容。";
        }

        String jsonCandidate = extractLastJsonObject(content);
        if (jsonCandidate == null) {
            return "输出中没有找到完整的 JSON 对象，或者 JSON 被额外文本/markdown 干扰。";
        }

        return describeJsonCandidateFailure(jsonCandidate);
    }

    /**
     * 判断结构化 JSON 是否已经开始输出。
     */
    public boolean hasStructuredJsonStarted(String content) {
        if (!hasText(content)) {
            return false;
        }

        String candidate = content.stripLeading();
        if (candidate.startsWith(MARKDOWN_JSON_FENCE) || candidate.startsWith(MARKDOWN_FENCE)) {
            return true;
        }
        return candidate.startsWith("{");
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
            JsonNode jsonNode = objectMapper.readTree(normalizeRecommendationJson(json));
            return normalizeText(jsonNode.path("replyText").asText(null));
        }
        catch (Exception e) {
            log.debug("Failed to extract replyText from structured JSON", e);
            return null;
        }
    }

    /**
     * 宽松提取 replyText。
     * 适用于“type + selectedIds + replyText”这类控制 JSON，不要求 recommendations 满足前端渲染结构。
     */
    public String extractLooseReplyText(String content) {
        String json = extractLastJsonObjectCandidate(content);
        if (json == null) {
            return null;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            return normalizeText(jsonNode.path("replyText").asText(null));
        }
        catch (Exception e) {
            log.debug("Failed to extract loose replyText from control JSON", e);
            return null;
        }
    }

    /**
     * 提取最后一个原始 JSON 对象候选。
     * 不校验前端结构，只做 JSON 片段抽取。
     */
    public String extractLastJsonObjectCandidate(String content) {
        if (!hasText(content)) {
            return null;
        }
        return extractLastJsonObject(content);
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
            JsonNode parsed = objectMapper.readTree(normalizeRecommendationJson(json));
            if (!(parsed instanceof ObjectNode root)) {
                return null;
            }

            String type = resolveStructuredType(root);
            String replyText = normalizeText(root.path("replyText").asText(null));
            String structuredSummary = buildStructuredSummary(root, type);
            if (!hasText(replyText)) {
                return structuredSummary;
            }
            if (!hasText(structuredSummary)) {
                return "replyText=" + replyText;
            }
            return "replyText=" + replyText + "; " + structuredSummary;
        }
        catch (Exception e) {
            log.debug("Failed to build memory summary from structured JSON", e);
            return extractReplyText(content);
        }
    }

    /**
     * 规范化结构化 JSON。
     */
    public String normalizeRecommendationJson(String json) {
        StructuredResponseParseResult parseResult = parseStructuredResponse(json);
        if (parseResult != null) {
            return parseResult.normalizedJson;
        }

        try {
            JsonNode parsed = objectMapper.readTree(json);
            if (!(parsed instanceof ObjectNode root)) {
                return json;
            }

            String type = resolveStructuredType(root);
            if (!hasText(type)) {
                return json;
            }

            root.put("type", type);

            if ("order".equals(type)) {
                JsonNode orderIdNode = root.get("orderId");
                if (orderIdNode != null && !orderIdNode.isNull()) {
                    root.put("orderId", orderIdNode.asText());
                }
            }

            JsonNode recommendationsNode = root.get("recommendations");
            if (("shop".equals(type) || "product".equals(type)) && recommendationsNode instanceof ArrayNode recommendations) {
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
            log.warn("Failed to normalize structured JSON, keep original", e);
            return json;
        }
    }

    /**
     * 构建结构化摘要。
     */
    private String buildStructuredSummary(ObjectNode root, String type) {
        if ("order".equals(type)) {
            String orderId = normalizeNodeValue(root.get("orderId"));
            return hasText(orderId) ? "orderId=" + orderId : null;
        }
        if ("order_fail".equals(type)) {
            return "orderStatus=failed";
        }
        return buildRecommendationSummary(root, type);
    }

    /**
     * 构建推荐摘要。
     */
    private String buildRecommendationSummary(ObjectNode root, String type) {
        JsonNode recommendationsNode = root.get("recommendations");
        if (!(recommendationsNode instanceof ArrayNode recommendations) || recommendations.isEmpty()) {
            return null;
        }

        boolean productRecommendation = "product".equals(type) || isProductRecommendation(root, recommendations);
        List<String> summaries = new ArrayList<>();
        for (JsonNode node : recommendations) {
            if (!(node instanceof ObjectNode recommendation)) {
                continue;
            }

            String summary = productRecommendation
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
        String type = resolveStructuredType(root);
        if ("product".equals(type)) {
            return true;
        }
        if ("shop".equals(type)) {
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
     * 解析结构化响应。
     */
    private StructuredResponseParseResult parseStructuredResponse(String content) {
        if (!hasText(content) || !content.startsWith("{") || !content.endsWith("}")) {
            return null;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(content);
            if (!(jsonNode instanceof ObjectNode root)) {
                return null;
            }
            return parseStructuredResponse(root);
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析结构化响应。
     */
    private StructuredResponseParseResult parseStructuredResponse(ObjectNode root) {
        String type = resolveStructuredType(root);
        if (!hasText(type)) {
            return null;
        }

        try {
            ObjectNode normalizedRoot = root.deepCopy();
            normalizedRoot.put("type", type);
            if ("order".equals(type)) {
                String orderId = normalizeNodeValue(normalizedRoot.get("orderId"));
                if (hasText(orderId)) {
                    normalizedRoot.put("orderId", orderId);
                }
            }

            BaseStructuredResponseDTO dto = convertToStructuredDto(type, normalizedRoot);
            if (!hasValidReplyText(dto)) {
                return null;
            }

            switch (type) {
                case "shop" -> {
                    if (!(dto instanceof ShopCardResponseDTO shopCardResponseDTO) || shopCardResponseDTO.getRecommendations() == null) {
                        return null;
                    }
                }
                case "product" -> {
                    if (!(dto instanceof ProductCardResponseDTO productCardResponseDTO) || productCardResponseDTO.getRecommendations() == null) {
                        return null;
                    }
                }
                case "order" -> {
                    if (!(dto instanceof OrderCardResponseDTO orderCardResponseDTO) || !hasText(orderCardResponseDTO.getOrderId())) {
                        return null;
                    }
                }
                case "order_fail" -> {
                    if (!(dto instanceof OrderFailCardResponseDTO)) {
                        return null;
                    }
                }
                default -> {
                    return null;
                }
            }

            JsonNode recommendationsNode = normalizedRoot.get("recommendations");
            if (("shop".equals(type) || "product".equals(type)) && recommendationsNode instanceof ArrayNode recommendations) {
                for (JsonNode node : recommendations) {
                    if (!(node instanceof ObjectNode recommendation)) {
                        continue;
                    }
                    if (!hasText(recommendation.path("type").asText(null))) {
                        recommendation.put("type", type);
                    }
                }
            }

            return new StructuredResponseParseResult(type, dto, objectMapper.writeValueAsString(normalizedRoot));
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * 描述 JSON 候选的校验失败原因。
     */
    private String describeJsonCandidateFailure(String jsonCandidate) {
        if (!hasText(jsonCandidate)) {
            return "模型没有返回任何结构化内容。";
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(jsonCandidate);
            if (!(jsonNode instanceof ObjectNode root)) {
                return "JSON 根节点不是对象。";
            }

            String type = resolveStructuredType(root);
            if (!hasText(type)) {
                return "无法识别结构化类型，必须输出 shop、product、order 或 order_fail。";
            }

            ObjectNode normalizedRoot = root.deepCopy();
            normalizedRoot.put("type", type);
            if ("order".equals(type)) {
                String orderId = normalizeNodeValue(normalizedRoot.get("orderId"));
                if (hasText(orderId)) {
                    normalizedRoot.put("orderId", orderId);
                }
            }

            BaseStructuredResponseDTO dto = convertToStructuredDto(type, normalizedRoot);
            if (!hasValidReplyText(dto)) {
                return "缺少 replyText 字段，或 replyText 为空。";
            }

            return switch (type) {
                case "shop" -> dto instanceof ShopCardResponseDTO shopCardResponseDTO && shopCardResponseDTO.getRecommendations() != null
                        ? null
                        : "recommendations 字段缺失，或 recommendations 不是数组。";
                case "product" -> dto instanceof ProductCardResponseDTO productCardResponseDTO && productCardResponseDTO.getRecommendations() != null
                        ? null
                        : "recommendations 字段缺失，或 recommendations 不是数组。";
                case "order" -> dto instanceof OrderCardResponseDTO orderCardResponseDTO && hasText(orderCardResponseDTO.getOrderId())
                        ? null
                        : "orderId 字段缺失，或 orderId 为空。";
                case "order_fail" -> dto instanceof OrderFailCardResponseDTO
                        ? null
                        : "order_fail 结构不合法，请重新输出。";
                default -> "type 字段非法，必须是 shop、product、order 或 order_fail。";
            };
        }
        catch (Exception e) {
            return "JSON 无法解析或无法映射到结构化 DTO，请重新输出一个合法 JSON 对象。";
        }
    }

    /**
     * 转换为结构化 DTO。
     */
    private BaseStructuredResponseDTO convertToStructuredDto(String type, ObjectNode normalizedRoot) throws Exception {
        return switch (type) {
            case "shop" -> objectMapper.treeToValue(normalizedRoot, ShopCardResponseDTO.class);
            case "product" -> objectMapper.treeToValue(normalizedRoot, ProductCardResponseDTO.class);
            case "order" -> objectMapper.treeToValue(normalizedRoot, OrderCardResponseDTO.class);
            case "order_fail" -> objectMapper.treeToValue(normalizedRoot, OrderFailCardResponseDTO.class);
            default -> null;
        };
    }

    /**
     * 是否存在有效回复文本。
     */
    private boolean hasValidReplyText(BaseStructuredResponseDTO dto) {
        return dto != null && hasText(normalizeText(dto.getReplyText()));
    }

    /**
     * 提取最后一个完整 JSON 对象。
     */
    private String extractLastJsonObject(String content) {
        String prepared = stripTrailingMarkdownFence(stripLeadingMarkdownFence(content));
        int end = prepared.lastIndexOf("}");
        if (end < 0) {
            return null;
        }

        int braceCount = 0;
        int start = -1;
        for (int i = end; i >= 0; i--) {
            char c = prepared.charAt(i);
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
        return prepared.substring(start, end + 1);
    }

    /**
     * 解析结构化响应类型。
     */
    private String resolveStructuredType(ObjectNode root) {
        String rootType = normalizeType(root.path("type").asText(null));
        if (hasText(rootType)) {
            return rootType;
        }

        if (hasText(normalizeNodeValue(root.get("orderId")))) {
            return "order";
        }

        if (root.get("recommendations") instanceof ArrayNode recommendations) {
            for (JsonNode node : recommendations) {
                if (!(node instanceof ObjectNode recommendation)) {
                    continue;
                }

                String itemType = normalizeType(recommendation.path("type").asText(null));
                if (hasText(itemType)) {
                    return itemType;
                }

                if (looksLikeProductRecommendation(recommendation)) {
                    return "product";
                }
                if (looksLikeShopRecommendation(recommendation)) {
                    return "shop";
                }
            }
            return "shop";
        }

        return null;
    }

    /**
     * 规范化响应类型。
     */
    private String normalizeType(String type) {
        if (!hasText(type)) {
            return null;
        }
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        if ("voucher".equals(normalized)) {
            return "product";
        }
        return normalized;
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
     * 移除开头 markdown fence。
     */
    private String stripLeadingMarkdownFence(String content) {
        if (content == null) {
            return "";
        }
        String stripped = content.stripLeading();
        if (stripped.startsWith(MARKDOWN_JSON_FENCE)) {
            return stripped.substring(MARKDOWN_JSON_FENCE.length()).stripLeading();
        }
        if (stripped.startsWith(MARKDOWN_FENCE)) {
            return stripped.substring(MARKDOWN_FENCE.length()).stripLeading();
        }
        return stripped;
    }

    /**
     * 移除结尾 markdown fence。
     */
    private String stripTrailingMarkdownFence(String content) {
        if (content == null) {
            return "";
        }
        String stripped = content.stripTrailing();
        if (stripped.endsWith(MARKDOWN_FENCE)) {
        return stripped.substring(0, stripped.lastIndexOf(MARKDOWN_FENCE)).stripTrailing();
        }
        return stripped;
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

    /**
     * 结构化解析结果。
     */
    private static class StructuredResponseParseResult {
        private final String type;
        private final BaseStructuredResponseDTO dto;
        private final String normalizedJson;

        private StructuredResponseParseResult(String type, BaseStructuredResponseDTO dto, String normalizedJson) {
            this.type = type;
            this.dto = dto;
            this.normalizedJson = normalizedJson;
        }
    }
}
