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

/**
 * 商品 RAG (检索增强生成) 服务
 * 负责在 Milvus 向量库中检索相关商品、代金券，并提供下单等功能
 *
 * @author smartLive
 */
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

    /**
     * 搜索商品列表
     * 根据用户输入的语义和过滤条件，在向量库中进行相似度检索
     *
     * @param productVo 过滤条件（如 shopId, category 等）
     * @param userMessage 用户搜索的文本消息
     * @return 检索到的商品 VO 列表
     */
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
        log.info("RAG 检索过滤条件: {}", filter);
        List<Document> results = productVectorStore.similaritySearch(builder.build());
        log.info("商品 RAG 检索结果: {}", results);
        return convertDocumentsToProductVO(results);
    }
    /**
     * 在对话中直接下单/抢购商品
     * 通过异步调用远程产品服务实现下单操作
     *
     * @param productVo 包含产品 ID 和用户 ID 的对象
     * @return 下单结果提示信息
     */
    @Override
    public String orderProduct(ProductVO productVo) {
        Long productVoId = productVo.getId();
        Long userId = productVo.getUserId();
        try {
            Long result = null;
            // 统一购买接口
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                return remoteProductService.purchaseProduct(productVoId, userId);
            });
            result = future.get();
            if (result != null ) {
                return "抢购成功！您的订单已生成，订单ID为：" + result;

            } else {
                return  "抢购失败";
            }
        } catch (Exception e) {
            log.error("抢购失败", e);
            return "抢购失败";
        }
    }

    /**
     * 构建向量检索的过滤表达式
     * 将 ProductVO 中的字段转化为 Spring AI 支持的过滤语法
     *
     * @param productVO 包含过滤参数的对象
     * @return 过滤表达式字符串
     */
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

    /**
     * 转义过滤值中的特殊字符
     */
    private String escapeForFilter(String input) {
        return input.replace("'", "\\'");
    }

    /**
     * 批量转换 Document 为 ProductVO
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
     * 将 Spring AI 的 Document 将元数据映射为 ProductVO 实体
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
            // 时间字段处理
            product.setBeginTime(RagMetadataValueUtils.parseDate(RagMetadataValueUtils.toStringValue(metadata.get("beginTime"))));
            product.setEndTime(RagMetadataValueUtils.parseDate(RagMetadataValueUtils.toStringValue(metadata.get("endTime"))));
            product.setValidityType(RagMetadataValueUtils.toInteger(metadata.get("validityType")));
            product.setUseStartTime(RagMetadataValueUtils.parseDate(RagMetadataValueUtils.toStringValue(metadata.get("useStartTime"))));
            product.setUseEndTime(RagMetadataValueUtils.parseDate(RagMetadataValueUtils.toStringValue(metadata.get("useEndTime"))));
            product.setValidDays(RagMetadataValueUtils.toInteger(metadata.get("validDays")));
            return product;
        } catch (Exception e) {
            log.warn("Document 转换为 ProductVO 失败: {}", e.getMessage());
            return null;
        }
    }
}

