package com.smartLive.ai.service.rag.impl;

import com.smartLive.ai.entity.vo.ProductVO;
import com.smartLive.ai.service.rag.IProductRagService;
import com.smartLive.product.api.RemoteProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class ProductRagService implements IProductRagService {

    private final VectorStore productVectorStore;
    private final RemoteProductService remoteProductService;
    @Autowired
    public ProductRagService(@Qualifier("productVectorStore") VectorStore vectorStore,
                             RemoteProductService remoteProductService) {
        this.productVectorStore = vectorStore;
        this.remoteProductService = remoteProductService;
    }

    @Override
    public List<ProductVO> getProductList(ProductVO productVo, String userMessage) {
        String ragQuery = (userMessage == null || userMessage.isBlank()) ? "product coupon" : userMessage;
        String filter = buildFilterExpression(productVo);

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(ragQuery)
                .topK(5);
        if (StringUtils.hasText(filter)) {
            builder.filterExpression(filter);
        }

        List<Document> results = productVectorStore.similaritySearch(builder.build());
        log.info("Product RAG search results: {}", results);
        return convertDocumentsToProductVO(results);
    }

    @Override
    public String orderProduct(ProductVO productVo) {
        Long userId = productVo.getUserId();
        List<Document> results = productVectorStore.similaritySearch(
                SearchRequest.builder()
                        .filterExpression(buildFilterExpression(productVo))
                        .topK(1)
                        .build());
        log.info("🔍 RAG搜索结果：{}", results);
        //要下单的优惠券
        ProductVO vo = convertDocumentToProductVO(results.get(0));
        if (vo == null) {
            return "没有找到该商品";
        }
        log.info("下单的商品为" + vo);
        if (vo.getActivityType() == 1 && vo.getStock() <= 0) {
            log.info("该商品已售罄");
            return "该商品已售罄";
        }
        try {
            Long result = null;
            // Unified purchase interface
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                return remoteProductService.purchaseProduct(vo.getId(), userId);
            });
            result = future.get();
            if (result != null ) {
                return "抢购成功，订单id为" + result;
            } else {
                return  "抢购失败";
            }
        } catch (Exception e) {
            log.error("抢购失败", e);
            return "抢购失败";
        }
    }

    private String buildFilterExpression(ProductVO productVO) {
        List<String> filters = new ArrayList<>();
        filters.add("status == 1");

        if (productVO != null) {
            if (StringUtils.hasText(productVO.getShopName())) {
                filters.add(String.format("shopName == '%s'", escapeForFilter(productVO.getShopName())));
            }
            if (productVO.getActivityType() != null) {
                filters.add("activityType == " + productVO.getActivityType());
            }
            if (productVO.getTypeId() != null) {
                filters.add("typeId == " + productVO.getTypeId());
            }
            if (StringUtils.hasText(productVO.getName())) {
                filters.add(String.format("name == '%s'", escapeForFilter(productVO.getName())));
            }
            if (productVO.getId() != null) {
                filters.add("id == " + productVO.getId());
            }
            if (productVO.getShopId() != null && !productVO.getShopId().isEmpty()) {
                filters.add("shopId == " + productVO.getShopId().split(",")[0]);
            }
        }
        return String.join(" && ", filters);
    }

    private String escapeForFilter(String input) {
        return input.replace("'", "\\'");
    }

    private List<ProductVO> convertDocumentsToProductVO(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        return documents.stream()
                .map(this::convertDocumentToProductVO)
                .filter(Objects::nonNull)
                .toList();
    }

    private ProductVO convertDocumentToProductVO(Document document) {
        try {
            Map<String, Object> metadata = document.getMetadata();
            ProductVO product = new ProductVO();

            product.setId(toLong(metadata.get("id")));
            if (metadata.get("shopId") != null) {
                product.setShopId(metadata.get("shopId").toString());
            }
            product.setShopName(toStringValue(metadata.get("shopName")));
            product.setTypeId(toLong(metadata.get("typeId")));
            product.setName(toStringValue(metadata.get("name"))); // Updated to name
            product.setSubTitle(toStringValue(metadata.get("subTitle")));
            product.setRulesJson(toStringValue(metadata.get("rulesJson")));
            product.setPrice(toBigDecimal(metadata.get("price")));
            product.setOriginalPrice(toBigDecimal(metadata.get("originalPrice")));
            product.setActivityType(toInteger(metadata.get("activityType")));
            product.setStatus(toInteger(metadata.get("status")));
            product.setStock(toInteger(metadata.get("stock")));
            product.setBeginTime(toLocalDateTime(metadata.get("beginTime")));
            product.setEndTime(toLocalDateTime(metadata.get("endTime")));

            return product;
        } catch (Exception e) {
            log.warn("Failed to convert Document to ProductVO: {}", e.getMessage());
            return null;
        }
    }

    private String toStringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
    }

    private Long toLong(Object value) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal == null ? null : decimal.longValue();
    }

    private Integer toInteger(Object value) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal == null ? null : decimal.intValue();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            log.warn("Cannot parse numeric metadata value: {}", text);
            return null;
        }
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return null;
        }

        BigDecimal decimal = toBigDecimal(value);
        if (decimal != null) {
            long epoch = decimal.longValue();
            if (Math.abs(epoch) < 100_000_000_000L) {
                return LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault());
            }
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault());
        }

        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(text), ZoneId.systemDefault());
        } catch (DateTimeParseException ex) {
            log.warn("Cannot parse time metadata value: {}", text);
            return null;
        }
    }
}
