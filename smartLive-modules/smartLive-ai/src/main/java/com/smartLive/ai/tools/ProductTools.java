package com.smartLive.ai.tools;

import com.smartLive.ai.entity.vo.ProductVO;
import com.smartLive.ai.service.rag.IProductRagService;
import com.smartLive.ai.service.user.support.StructuredToolCaptureRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 商品工具集。
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ProductTools {

    private final IProductRagService productRagService;
    private final StructuredToolCaptureRegistry structuredToolCaptureRegistry;

    @Tool(name = "listProduct", description = "List products by shop, category or activity type.")
    public List<ProductVO> listProduct(
            @ToolParam(description = "Original user message.", required = false)
            String userMessage,
            @ToolParam(description = "Shop id.", required = false)
            String shopId,
            @ToolParam(description = "Product category.", required = false)
            Integer category,
            @ToolParam(description = "Activity type.", required = false)
            Integer type,
            ToolContext toolContext
    ) {
        ProductVO product = new ProductVO();
        product.setShopId(shopId);
        product.setActivityType(type);
        product.setCategory(category);
        log.info("Calling listProduct | shopId={}, category={}, type={}", shopId, category, type);
        List<ProductVO> productList = productRagService.getProductList(product, userMessage);
        structuredToolCaptureRegistry.recordProductResults(toolContext, productList);
        productList.forEach(productVO -> log.info("Product candidate: {}", productVO));
        return productList;
    }

    /**
     * 获取字符串结果。
     */
    @Tool(name = "orderProduct", description = "Create an order for a selected product.")
    public String orderProduct(
            @ToolParam(description = "Product id.") Long productId,
            @ToolParam(description = "Shop id.") String shopId,
            @ToolParam(description = "User id.") Long userId,
            ToolContext toolContext
    ) {
        log.info("Calling orderProduct | shopId={}, productId={}, userId={}", shopId, productId, userId);
        ProductVO query = new ProductVO();
        query.setId(productId);
        query.setShopId(shopId);
        query.setUserId(userId);
        String orderResult = productRagService.orderProduct(query);
        structuredToolCaptureRegistry.recordOrderResult(toolContext, orderResult);
        return orderResult;
    }
}
