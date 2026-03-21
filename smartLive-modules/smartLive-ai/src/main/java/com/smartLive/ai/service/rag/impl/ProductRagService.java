package com.smartLive.ai.service.rag.impl;

import com.smartLive.ai.entity.vo.ProductVO;
import com.smartLive.ai.service.rag.IProductRagService;
import com.smartLive.ai.utils.RagMetadataValueUtils;
import com.smartLive.product.api.DTO.ProductDTO;
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

/**
 * 商品 RAG 服务实现类。
 */
@Service
@Slf4j
public class ProductRagService implements IProductRagService {

    private final VectorStore productVectorStore;
    private final RemoteProductService remoteProductService;

    /**
     * 构造商品 RAG 服务实现类。
     */
    @Autowired
    public ProductRagService(@Qualifier("productVectorStore") VectorStore vectorStore,
                             RemoteProductService remoteProductService) {
        this.productVectorStore = vectorStore;
        this.remoteProductService = remoteProductService;
    }

    /**
     * 获取商品列表。
     */
    @Override
    public List<ProductVO> getProductList(ProductVO productVo, String userMessage) {
        if (productVo != null && productVo.getId() != null) {
            ProductDTO productDTO = remoteProductService.getProductById(productVo.getId());
            ProductVO product = convertProductDtoToProductVo(productDTO);
            if (product == null || !matchesRpcProductFilter(product, productVo)) {
                return List.of();
            }
            return List.of(product);
        }

        String ragQuery = (userMessage == null || userMessage.isBlank()) ? "product coupon" : userMessage;
        String filter = buildFilterExpression(productVo);

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(ragQuery)
                .topK(5);
        if (StringUtils.hasText(filter)) {
            builder.filterExpression(filter);
        }
        log.info("Product RAG filter: {}", filter);
        List<Document> results = productVectorStore.similaritySearch(builder.build());
        log.info("Product RAG search results: {}", results);
        return convertDocumentsToProductVO(results);
    }

    /**
     * 获取字符串结果。
     */
    @Override
    public String orderProduct(ProductVO productVo) {
        Long productId = productVo.getId();
        Long userId = productVo.getUserId();
        try {
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> remoteProductService.purchaseProduct(productId, userId));
            Long result = future.get();
            if (result != null) {
                // 返回 JSON 格式，orderId 用引号包裹为字符串，避免前端 JSON.parse 大整数精度丢失
                return "{\"success\": true, \"orderId\": \"" + result + "\"}";
            }
            return "{\"success\": false, \"message\": \"Purchase failed\"}";
        } catch (Exception e) {
            log.error("Purchase failed", e);
            return "{\"success\": false, \"message\": \"Purchase failed: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 获取结果。
     */
    private boolean matchesRpcProductFilter(ProductVO product, ProductVO query) {
        if (product == null || query == null) {
            return false;
        }
        if (StringUtils.hasText(query.getShopId()) && !query.getShopId().equals(product.getShopId())) {
            return false;
        }
        if (query.getActivityType() != null && !query.getActivityType().equals(product.getActivityType())) {
            return false;
        }
        if (query.getCategory() != null && !query.getCategory().equals(product.getCategory())) {
            return false;
        }
        if (StringUtils.hasText(query.getName()) && !query.getName().trim().equals(product.getName())) {
            return false;
        }
        return true;
    }

    /**
     * 构建过滤表达式。
     */
    private String buildFilterExpression(ProductVO productVO) {
        List<String> filters = new ArrayList<>();
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

    /**
     * 获取字符串结果。
     */
    private String escapeForFilter(String input) {
        return input.replace("'", "\\'");
    }

    /**
     * 转换商品数据传输对象商品视图对象。
     */
    private ProductVO convertProductDtoToProductVo(ProductDTO productDTO) {
        if (productDTO == null) {
            return null;
        }
        ProductVO product = new ProductVO();
        product.setId(productDTO.getId());
        product.setShopId(productDTO.getShopId());
        product.setName(productDTO.getName());
        product.setSubTitle(productDTO.getSubTitle());
        product.setRulesJson(productDTO.getRulesJson());
        product.setPrice(productDTO.getPrice());
        product.setOriginalPrice(productDTO.getOriginalPrice());
        product.setActivityType(productDTO.getActivityType());
        product.setCategory(productDTO.getCategory());
        product.setCoverImg(productDTO.getCoverImg());
        product.setStatus(productDTO.getStatus());
        product.setStock(productDTO.getStock());
        product.setBeginTime(productDTO.getBeginTime());
        product.setEndTime(productDTO.getEndTime());
        product.setValidityType(productDTO.getValidityType());
        product.setUseStartTime(productDTO.getUseStartTime());
        product.setUseEndTime(productDTO.getUseEndTime());
        product.setValidDays(productDTO.getValidDays());
        return product;
    }

    /**
     * 将文档列表转换为商品对象。
     */
    private List<ProductVO> convertDocumentsToProductVO(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        return documents.stream()
                .map(this::convertDocumentToProductVO)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 将文档转换为商品对象。
     */
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
            product.setBeginTime(RagMetadataValueUtils.parseDate(RagMetadataValueUtils.toStringValue(metadata.get("beginTime"))));
            product.setEndTime(RagMetadataValueUtils.parseDate(RagMetadataValueUtils.toStringValue(metadata.get("endTime"))));
            product.setValidityType(RagMetadataValueUtils.toInteger(metadata.get("validityType")));
            product.setUseStartTime(RagMetadataValueUtils.parseDate(RagMetadataValueUtils.toStringValue(metadata.get("useStartTime"))));
            product.setUseEndTime(RagMetadataValueUtils.parseDate(RagMetadataValueUtils.toStringValue(metadata.get("useEndTime"))));
            product.setValidDays(RagMetadataValueUtils.toInteger(metadata.get("validDays")));
            return product;
        } catch (Exception e) {
            log.warn("Failed to convert document to ProductVO: {}", e.getMessage());
            return null;
        }
    }
}