package com.smartLive.ai.service.rag.impl;

import com.smartLive.ai.entity.vo.ProductVO;
import com.smartLive.ai.service.rag.IProductRagService;
import com.smartLive.ai.utils.RagMetadataValueUtils;
import com.smartLive.product.api.RemoteProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
        log.info("RAG query: {}", filter);
        List<Document> results = productVectorStore.similaritySearch(builder.build());
        log.info("Product RAG search results: {}", results);
        return convertDocumentsToProductVO(results);
    }
    /**
     * 涓嬪崟
     */
    @Override
    public String orderProduct(ProductVO productVo) {
        Long productVoId = productVo.getId();
        Long userId = productVo.getUserId();
        try {
            Long result = null;
            // Unified purchase interface
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                return remoteProductService.purchaseProduct(productVoId, userId);
            });
            result = future.get();
            if (result != null ) {
                return "鎶㈣喘鎴愬姛锛佹偍鐨勮鍗曞凡鐢熸垚锛岃鍗旾D涓猴細" + result;

            } else {
                return  "鎶㈣喘澶辫触";
            }
        } catch (Exception e) {
            log.error("鎶㈣喘澶辫触", e);
            return "鎶㈣喘澶辫触";
        }
    }

    private String buildFilterExpression(ProductVO productVO) {
        List<String> filters = new ArrayList<>();
//        filters.add("status == 1");

        if (productVO != null) {
            if (productVO.getShopId() != null) {
                filters.add("shopId in [\"" + productVO.getShopId() + "\"]");
            }
            if (productVO.getActivityType() != null) {
                filters.add("activityType == " + productVO.getActivityType());
            }
            if (productVO.getCategory() != null) {
                filters.add("category == " + productVO.getCategory());
            }
            if (StringUtils.hasText(productVO.getName())) {
                filters.add(String.format("name == '%s'", escapeForFilter(productVO.getName())));
            }
            if (productVO.getId() != null) {
                filters.add("id == " + productVO.getId());
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

            product.setId(RagMetadataValueUtils.toLong(metadata.get("id")));
            if (metadata.get("shopId") != null) {
                product.setShopId(metadata.get("shopId").toString());
            }
            product.setName(RagMetadataValueUtils.toStringValue(metadata.get("name")));
            product.setSubTitle(RagMetadataValueUtils.toStringValue(metadata.get("subTitle")));
            product.setRulesJson(RagMetadataValueUtils.toStringValue(metadata.get("rulesJson")));
            product.setPrice(RagMetadataValueUtils.toBigDecimal(metadata.get("price")));
            product.setOriginalPrice(RagMetadataValueUtils.toBigDecimal(metadata.get("originalPrice")));
            product.setActivityType(RagMetadataValueUtils.toInteger(metadata.get("activityType")));
            product.setCategory(RagMetadataValueUtils.toInteger(metadata.get("category")));
            product.setStatus(RagMetadataValueUtils.toInteger(metadata.get("status")));
            product.setStock(RagMetadataValueUtils.toInteger(metadata.get("stock")));
            product.setCoverImg(RagMetadataValueUtils.toStringValue(metadata.get("coverImg")));
// 鉁?鏃堕棿瀛楁鍏ㄩ儴鏀圭敤 parseDate
            product.setBeginTime(RagMetadataValueUtils.parseDate(RagMetadataValueUtils.toStringValue(metadata.get("beginTime"))));
            product.setEndTime(RagMetadataValueUtils.parseDate(RagMetadataValueUtils.toStringValue(metadata.get("endTime"))));
            product.setValidityType(RagMetadataValueUtils.toInteger(metadata.get("validityType")));
            product.setUseStartTime(RagMetadataValueUtils.parseDate(RagMetadataValueUtils.toStringValue(metadata.get("useStartTime"))));
            product.setUseEndTime(RagMetadataValueUtils.parseDate(RagMetadataValueUtils.toStringValue(metadata.get("useEndTime"))));
            product.setValidDays(RagMetadataValueUtils.toInteger(metadata.get("validDays")));
            return product;
        } catch (Exception e) {
            log.warn("Failed to convert Document to ProductVO: {}", e.getMessage());
            return null;
        }
    }
}

