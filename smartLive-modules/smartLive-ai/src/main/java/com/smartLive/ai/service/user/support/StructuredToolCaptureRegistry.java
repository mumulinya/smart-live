package com.smartLive.ai.service.user.support;

import com.smartLive.ai.entity.vo.ProductVO;
import com.smartLive.ai.entity.vo.ShopVO;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 结构化工具调用结果捕获注册表。
 * 用于在“模型走工具”的链路里记录真实工具结果，供后端最终组装 card_render JSON。
 */
@Component
public class StructuredToolCaptureRegistry {

    public static final String REQUEST_ID_KEY = "structured_request_id";

    private final ConcurrentMap<String, StructuredToolCapture> captures = new ConcurrentHashMap<>();
    private final ThreadLocal<String> activeCaptureId = new ThreadLocal<>();

    /**
     * 打开一个新的捕获会话。
     */
    public String openCapture() {
        String requestId = UUID.randomUUID().toString();
        captures.put(requestId, new StructuredToolCapture());
        return requestId;
    }

    /**
     * 获取捕获结果。
     */
    public StructuredToolCapture getCapture(String requestId) {
        return requestId == null ? null : captures.get(requestId);
    }

    /**
     * 清理捕获结果。
     */
    public void clearCapture(String requestId) {
        if (requestId != null) {
            captures.remove(requestId);
        }
    }

    /**
     * 激活当前线程的捕获会话。
     */
    public void activateCapture(String requestId) {
        if (requestId != null) {
            activeCaptureId.set(requestId);
        }
    }

    /**
     * 清理当前线程的捕获会话。
     */
    public void clearActiveCapture() {
        activeCaptureId.remove();
    }

    /**
     * 记录店铺搜索结果。
     */
    public void recordShopResults(ToolContext toolContext, List<ShopVO> shops) {
        StructuredToolCapture capture = resolveCapture(toolContext);
        if (capture == null) {
            return;
        }
        capture.shopToolCalled = true;
        capture.shops = shops == null ? List.of() : new ArrayList<>(shops);
    }

    /**
     * 记录商品搜索结果。
     */
    public void recordProductResults(ToolContext toolContext, List<ProductVO> products) {
        StructuredToolCapture capture = resolveCapture(toolContext);
        if (capture == null) {
            return;
        }
        capture.productToolCalled = true;
        capture.products = products == null ? List.of() : new ArrayList<>(products);
    }

    /**
     * 记录下单结果。
     */
    public void recordOrderResult(ToolContext toolContext, String rawOrderResult) {
        StructuredToolCapture capture = resolveCapture(toolContext);
        if (capture == null) {
            return;
        }
        capture.orderToolCalled = true;
        capture.orderResult = rawOrderResult;
    }

    /**
     * 解析捕获对象。
     */
    private StructuredToolCapture resolveCapture(ToolContext toolContext) {
        if (toolContext != null && toolContext.getContext() != null) {
            Map<String, Object> context = toolContext.getContext();
            Object requestId = context.get(REQUEST_ID_KEY);
            if (requestId != null) {
                return captures.get(String.valueOf(requestId));
            }
        }

        String activeId = activeCaptureId.get();
        if (activeId == null) {
            return null;
        }
        return captures.get(activeId);
    }

    /**
     * 结构化捕获结果。
     */
    public static class StructuredToolCapture {
        private boolean shopToolCalled;
        private boolean productToolCalled;
        private boolean orderToolCalled;
        private List<ShopVO> shops = List.of();
        private List<ProductVO> products = List.of();
        private String orderResult;

        public boolean isShopToolCalled() {
            return shopToolCalled;
        }

        public boolean isProductToolCalled() {
            return productToolCalled;
        }

        public boolean isOrderToolCalled() {
            return orderToolCalled;
        }

        public List<ShopVO> getShops() {
            return shops;
        }

        public List<ProductVO> getProducts() {
            return products;
        }

        public String getOrderResult() {
            return orderResult;
        }
    }
}
