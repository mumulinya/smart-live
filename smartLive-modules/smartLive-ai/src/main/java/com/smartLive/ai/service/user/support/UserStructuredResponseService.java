package com.smartLive.ai.service.user.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.ai.domain.DTO.OrderCardResponseDTO;
import com.smartLive.ai.domain.DTO.OrderFailCardResponseDTO;
import com.smartLive.ai.domain.DTO.ProductCardResponseDTO;
import com.smartLive.ai.domain.DTO.RecommendationItemDTO;
import com.smartLive.ai.domain.DTO.ShopCardResponseDTO;
import com.smartLive.ai.entity.request.AIChatRequest;
import com.smartLive.ai.entity.vo.ProductVO;
import com.smartLive.ai.entity.vo.ShopVO;
import com.smartLive.ai.service.user.support.StructuredToolCaptureRegistry.StructuredToolCapture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 用户侧结构化响应服务。
 * 根据模型最终输出与工具真实返回值，组装最终 card_render JSON。
 */
@Slf4j
@Component
public class UserStructuredResponseService {

    private static final int DEFAULT_MAX_RECOMMENDATIONS = 5;
    private static final int MAX_RETRY_CONTEXT_ITEMS = 10;

    private final StructuredToolCaptureRegistry captureRegistry;
    private final RecommendationCardHelper recommendationCardHelper;
    private final ObjectMapper objectMapper;

    public UserStructuredResponseService(
            StructuredToolCaptureRegistry captureRegistry,
            RecommendationCardHelper recommendationCardHelper,
            ObjectMapper objectMapper
    ) {
        this.captureRegistry = captureRegistry;
        this.recommendationCardHelper = recommendationCardHelper;
        this.objectMapper = objectMapper;
    }

    /**
     * 基于工具真实结果组装结构化 JSON。
     */
    public String buildStructuredJsonFromCapture(
            String requestId,
            String userMessage,
            String modelResponse
    ) {
        return buildStructuredJson(requestId, userMessage, modelResponse);
    }

    /**
     * 为结构化重试构建工具真实结果上下文。
     * 这里返回的是给模型看的“证据摘要”，不是最终发给前端的 JSON。
     */
    public String buildRetryToolContext(String requestId) {
        StructuredToolCapture capture = captureRegistry.getCapture(requestId);
        if (capture == null) {
            return null;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        if (capture.isOrderToolCalled()) {
            payload.put("tool", "orderProduct");
            payload.put("kind", "order");
            payload.put("result", parseJsonOrRawText(capture.getOrderResult()));
            return writeAsJson(payload);
        }
        if (capture.isProductToolCalled()) {
            payload.put("tool", "listProduct");
            payload.put("kind", "product");
            payload.put("candidates", buildProductRetryCandidates(capture.getProducts()));
            return writeAsJson(payload);
        }
        if (capture.isShopToolCalled()) {
            payload.put("tool", "searchShopsByCategory");
            payload.put("kind", "shop");
            payload.put("candidates", buildShopRetryCandidates(capture.getShops()));
            return writeAsJson(payload);
        }
        return null;
    }

    /**
     * 基于工具真实结果组装结构化 JSON。
     */
    private String buildStructuredJson(String requestId, String userMessage, String modelResponse) {
        StructuredToolCapture capture = captureRegistry.getCapture(requestId);
        if (capture != null) {
            if (capture.isOrderToolCalled()) {
                String json = buildOrderJson(capture.getOrderResult(), modelResponse);
                if (hasText(json)) {
                    return json;
                }
            }
            if (capture.isProductToolCalled()) {
                return buildProductJson(capture.getProducts(), userMessage, modelResponse);
            }
            if (capture.isShopToolCalled()) {
                return buildShopJson(capture.getShops(), userMessage, modelResponse);
            }
        }

        String extractedJson = recommendationCardHelper.extractRecommendationJson(modelResponse);
        return hasText(extractedJson) ? recommendationCardHelper.normalizeRecommendationJson(extractedJson) : null;
    }

    /**
     * 构建店铺卡片 JSON。
     */
    private String buildShopJson(List<ShopVO> shops, String userMessage, String modelResponse) {
        ShopCardResponseDTO response = new ShopCardResponseDTO();
        response.setType("shop");
        response.setRecommendations(mapShopRecommendations(shops, modelResponse));
        response.setReplyText(resolveReplyText(
                "shop",
                response.getRecommendations().isEmpty(),
                modelResponse,
                response.getRecommendations().isEmpty()
                        ? "附近暂时没有找到符合条件的店铺，您可以换个商圈或品类再试试"
                        : defaultShopReplyText(response.getRecommendations(), userMessage)
        ));
        return writeAsJson(response);
    }

    /**
     * 构建商品卡片 JSON。
     */
    private String buildProductJson(List<ProductVO> products, String userMessage, String modelResponse) {
        ProductCardResponseDTO response = new ProductCardResponseDTO();
        response.setType("product");
        response.setRecommendations(mapProductRecommendations(products, modelResponse));
        response.setReplyText(resolveReplyText(
                "product",
                response.getRecommendations().isEmpty(),
                modelResponse,
                response.getRecommendations().isEmpty()
                        ? "当前没有找到符合条件的商品，您可以换个关键词再试试"
                        : defaultProductReplyText(response.getRecommendations(), userMessage)
        ));
        return writeAsJson(response);
    }

    /**
     * 构建订单结构化 JSON。
     */
    private String buildOrderJson(String rawOrderResult, String modelResponse) {
        if (!hasText(rawOrderResult)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(rawOrderResult);
            boolean success = root.path("success").asBoolean(false);
            if (success) {
                String orderId = root.path("orderId").asText(null);
                if (!hasText(orderId)) {
                    return null;
                }
                OrderCardResponseDTO response = new OrderCardResponseDTO();
                response.setType("order");
                response.setOrderId(orderId);
                response.setReplyText(resolveReplyText(
                        "order",
                        false,
                        modelResponse,
                        "下单成功，订单号如下"
                ));
                return writeAsJson(response);
            }

            String failureMessage = root.path("message").asText(null);
            OrderFailCardResponseDTO response = new OrderFailCardResponseDTO();
            response.setType("order_fail");
            response.setReplyText(resolveReplyText(
                    "order_fail",
                    true,
                    modelResponse,
                    hasText(failureMessage)
                            ? failureMessage
                            : "抱歉，下单失败，该商品可能已售罄或库存不足，建议稍后再试"
            ));
            return writeAsJson(response);
        }
        catch (Exception e) {
            log.warn("Failed to parse order tool result, fallback to raw model response");
            return null;
        }
    }

    /**
     * 解析 replyText。
     */
    private String resolveReplyText(String type, boolean emptyResult, String modelResponse, String fallbackText) {
        String extractedReplyText = recommendationCardHelper.extractReplyText(modelResponse);
        if (!hasText(extractedReplyText)) {
            extractedReplyText = recommendationCardHelper.extractLooseReplyText(modelResponse);
        }
        if (hasText(extractedReplyText)) {
            return extractedReplyText;
        }

        String normalizedText = normalizePlainText(modelResponse);
        if (hasText(normalizedText) && !looksLikeJsonText(normalizedText)) {
            return normalizedText;
        }

        if ("order_fail".equals(type) && emptyResult) {
            return fallbackText;
        }
        return fallbackText;
    }

    /**
     * 映射店铺推荐项。
     */
    private List<RecommendationItemDTO> mapShopRecommendations(List<ShopVO> shops, String modelResponse) {
        List<ShopVO> selectedShops = selectShops(shops, modelResponse);
        if (selectedShops.isEmpty()) {
            return List.of();
        }
        List<RecommendationItemDTO> items = new ArrayList<>();
        for (ShopVO shop : selectedShops) {
            if (shop == null) {
                continue;
            }
            RecommendationItemDTO item = new RecommendationItemDTO();
            item.setType("shop");
            item.setId(shop.getId());
            item.setName(shop.getName());
            item.setImages(shop.getImages());
            item.setShopLogo(shop.getShopLogo());
            item.setArea(shop.getArea());
            item.setAddress(shop.getAddress());
            item.setX(shop.getX());
            item.setY(shop.getY());
            item.setAvgPrice(shop.getAvgPrice());
            item.setSold(shop.getSold());
            item.setScore(shop.getScore());
            item.setOpenHours(shop.getOpenHours());
            item.setDistanceText(shop.getDistanceText());
            items.add(item);
        }
        return items;
    }

    /**
     * 映射商品推荐项。
     */
    private List<RecommendationItemDTO> mapProductRecommendations(List<ProductVO> products, String modelResponse) {
        List<ProductVO> selectedProducts = selectProducts(products, modelResponse);
        if (selectedProducts.isEmpty()) {
            return List.of();
        }
        List<RecommendationItemDTO> items = new ArrayList<>();
        for (ProductVO product : selectedProducts) {
            if (product == null) {
                continue;
            }
            RecommendationItemDTO item = new RecommendationItemDTO();
            item.setType("product");
            item.setId(product.getId());
            item.setShopId(product.getShopId());
            item.setName(product.getName());
            item.setSubTitle(product.getSubTitle());
            item.setPrice(product.getPrice());
            item.setOriginalPrice(product.getOriginalPrice());
            item.setActivityType(product.getActivityType());
            item.setCategory(product.getCategory());
            item.setCoverImg(product.getCoverImg());
            item.setStock(product.getStock());
            item.setBeginTime(product.getBeginTime());
            item.setEndTime(product.getEndTime());
            item.setValidityType(product.getValidityType());
            item.setUseStartTime(product.getUseStartTime());
            item.setUseEndTime(product.getUseEndTime());
            item.setValidDays(product.getValidDays());
            items.add(item);
        }
        return items;
    }

    /**
     * 店铺候选选择。
     * 如果模型明确返回了 recommendations，则以后端候选中与模型选择 id 对齐的结果为准。
     * 否则回退到默认前 N 条候选。
     */
    private List<ShopVO> selectShops(List<ShopVO> shops, String modelResponse) {
        if (shops == null || shops.isEmpty()) {
            return List.of();
        }

        StructuredSelection selection = extractSelection(modelResponse, "shop");
        if (!selection.present()) {
            return shops.stream().limit(DEFAULT_MAX_RECOMMENDATIONS).toList();
        }

        Map<String, ShopVO> candidateMap = new LinkedHashMap<>();
        for (ShopVO shop : shops) {
            if (shop == null || shop.getId() == null) {
                continue;
            }
            candidateMap.putIfAbsent(String.valueOf(shop.getId()), shop);
        }

        List<ShopVO> selected = new ArrayList<>();
        for (String selectedId : selection.selectedIds()) {
            ShopVO shop = candidateMap.get(selectedId);
            if (shop != null) {
                selected.add(shop);
            }
        }

        log.info("Structured shop selection resolved | modelSelected={}, matched={}",
                selection.selectedIds().size(), selected.size());
        return selected;
    }

    /**
     * 商品候选选择。
     * 如果模型明确返回了 recommendations，则以后端候选中与模型选择 id 对齐的结果为准。
     * 否则回退到默认前 N 条候选。
     */
    private List<ProductVO> selectProducts(List<ProductVO> products, String modelResponse) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }

        StructuredSelection selection = extractSelection(modelResponse, "product");
        if (!selection.present()) {
            return products.stream().limit(DEFAULT_MAX_RECOMMENDATIONS).toList();
        }

        Map<String, ProductVO> candidateMap = new LinkedHashMap<>();
        for (ProductVO product : products) {
            if (product == null || product.getId() == null) {
                continue;
            }
            candidateMap.putIfAbsent(String.valueOf(product.getId()), product);
        }

        List<ProductVO> selected = new ArrayList<>();
        for (String selectedId : selection.selectedIds()) {
            ProductVO product = candidateMap.get(selectedId);
            if (product != null) {
                selected.add(product);
            }
        }

        log.info("Structured product selection resolved | modelSelected={}, matched={}",
                selection.selectedIds().size(), selected.size());
        return selected;
    }

    /**
     * 从模型结构化输出中提取选择结果。
     * present=true 代表模型明确给出了 recommendations 数组，此时返回数量以后端可匹配到的 id 为准。
     * present=false 代表本次没有可靠选择信号，回退到默认前 N 条候选。
     */
    private StructuredSelection extractSelection(String modelResponse, String expectedType) {
        String json = recommendationCardHelper.extractLastJsonObjectCandidate(modelResponse);
        if (!hasText(json)) {
            return StructuredSelection.absent();
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            String responseType = normalizeStructuredType(root.path("type").asText(null));
            if (hasText(responseType) && !expectedType.equals(responseType)) {
                return StructuredSelection.absent();
            }

            LinkedHashSet<String> selectedIds = new LinkedHashSet<>();
            JsonNode selectedIdsNode = root.get("selectedIds");
            if (selectedIdsNode != null && selectedIdsNode.isArray()) {
                for (JsonNode itemNode : selectedIdsNode) {
                    String selectedId = normalizeSelectionId(itemNode);
                    if (hasText(selectedId)) {
                        selectedIds.add(selectedId);
                    }
                }
                return StructuredSelection.present(new ArrayList<>(selectedIds));
            }

            JsonNode recommendationsNode = root.get("recommendations");
            if (recommendationsNode != null && recommendationsNode.isArray()) {
                for (JsonNode itemNode : recommendationsNode) {
                    String selectedId = normalizeSelectionId(itemNode == null ? null : itemNode.get("id"));
                    if (hasText(selectedId)) {
                        selectedIds.add(selectedId);
                    }
                }
                return StructuredSelection.present(new ArrayList<>(selectedIds));
            }
            return StructuredSelection.absent();
        }
        catch (Exception e) {
            log.debug("Failed to extract model-selected recommendation ids from structured response", e);
            return StructuredSelection.absent();
        }
    }

    /**
     * 默认店铺推荐语。
     */
    private String defaultShopReplyText(List<RecommendationItemDTO> recommendations, String userMessage) {
        if (containsAny(normalize(userMessage), "火锅", "烧烤", "川菜", "湘菜", "奶茶", "咖啡")) {
            return "按您的口味偏好挑了几家更匹配的店铺，优先兼顾距离和口碑";
        }
        if (recommendations.size() == 1) {
            return "为您找到一家更匹配的店铺，口碑和距离都比较合适";
        }
        return "为您挑了几家更匹配的店铺，优先兼顾距离和口碑";
    }

    /**
     * 默认商品推荐语。
     */
    private String defaultProductReplyText(List<RecommendationItemDTO> recommendations, String userMessage) {
        if (containsAny(normalize(userMessage), "秒杀", "抢", "限时")) {
            return "为您筛了几款当前更值得关注的商品，优先考虑价格和可用性";
        }
        if (recommendations.size() == 1) {
            return "为您找到一个更匹配的商品，价格和适用性都比较合适";
        }
        return "为您筛了几款更匹配的商品，优先考虑价格和可用性";
    }

    /**
     * 店铺重试候选摘要。
     */
    private List<Map<String, Object>> buildShopRetryCandidates(List<ShopVO> shops) {
        if (shops == null || shops.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (ShopVO shop : shops.stream().limit(MAX_RETRY_CONTEXT_ITEMS).toList()) {
            if (shop == null || shop.getId() == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", shop.getId());
            putIfHasText(item, "name", shop.getName());
            putIfHasText(item, "area", shop.getArea());
            putIfHasText(item, "district", shop.getDistrict());
            putIfHasText(item, "address", shop.getAddress());
            putIfNotNull(item, "avgPrice", shop.getAvgPrice());
            putIfNotNull(item, "score", shop.getScore());
            putIfHasText(item, "distanceText", shop.getDistanceText());
            putIfHasText(item, "openHours", shop.getOpenHours());
            putIfNotNull(item, "typeId", shop.getTypeId());
            candidates.add(item);
        }
        return candidates;
    }

    /**
     * 商品重试候选摘要。
     */
    private List<Map<String, Object>> buildProductRetryCandidates(List<ProductVO> products) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (ProductVO product : products.stream().limit(MAX_RETRY_CONTEXT_ITEMS).toList()) {
            if (product == null || product.getId() == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", product.getId());
            putIfHasText(item, "name", product.getName());
            putIfHasText(item, "subTitle", product.getSubTitle());
            putIfNotNull(item, "price", product.getPrice());
            putIfNotNull(item, "originalPrice", product.getOriginalPrice());
            putIfNotNull(item, "activityType", product.getActivityType());
            putIfNotNull(item, "category", product.getCategory());
            putIfHasText(item, "shopId", product.getShopId());
            putIfNotNull(item, "stock", product.getStock());
            candidates.add(item);
        }
        return candidates;
    }

    /**
     * 解析 JSON 文本，失败时回退成原始字符串。
     */
    private Object parseJsonOrRawText(String value) {
        if (!hasText(value)) {
            return "";
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception ignored) {
            return value;
        }
    }

    /**
     * 规范化纯文本。
     */
    private String normalizePlainText(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.replace("\r", "\n")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 判断是否像 JSON。
     */
    private boolean looksLikeJsonText(String value) {
        if (!hasText(value)) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[") || trimmed.contains("\"replyText\"");
    }

    /**
     * 规范化文本。
     */
    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 判断是否包含任一关键词。
     */
    private boolean containsAny(String text, String... keywords) {
        if (!hasText(text) || keywords == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 规范化结构化类型。
     */
    private String normalizeStructuredType(String value) {
        if (!hasText(value)) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return "voucher".equals(normalized) ? "product" : normalized;
    }

    /**
     * 规范化模型选择的推荐项 id。
     */
    private String normalizeSelectionId(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            String value = node.asText();
            return hasText(value) ? value.trim() : null;
        }
        if (node.isIntegralNumber()) {
            return node.asText();
        }
        if (node.isFloatingPointNumber()) {
            BigDecimal decimal = node.decimalValue();
            try {
                return decimal.stripTrailingZeros().toPlainString();
            }
            catch (Exception ignored) {
                return node.asText();
            }
        }
        return null;
    }

    /**
     * 判断文本是否存在。
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 仅在值存在时放入字符串字段。
     */
    private void putIfHasText(Map<String, Object> target, String key, String value) {
        if (target == null || !hasText(value)) {
            return;
        }
        target.put(key, value.trim());
    }

    /**
     * 仅在值非空时放入字段。
     */
    private void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (target == null || value == null) {
            return;
        }
        target.put(key, value);
    }

    /**
     * 输出 JSON。
     */
    private String writeAsJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        }
        catch (Exception e) {
            throw new IllegalStateException("Failed to serialize structured response", e);
        }
    }

    /**
     * 结构化选择结果。
     */
    private record StructuredSelection(boolean present, List<String> selectedIds) {
        private static StructuredSelection absent() {
            return new StructuredSelection(false, List.of());
        }

        private static StructuredSelection present(List<String> selectedIds) {
            return new StructuredSelection(true, selectedIds == null ? List.of() : List.copyOf(selectedIds));
        }
    }
}
