package com.smartLive.ai.service.rag;

import com.smartLive.ai.entity.vo.ProductVO;

import java.util.List;

/**
 * Product RAG service.
 */
public interface IProductRagService {

    /**
     * Query products.
     */
    List<ProductVO> getProductList(ProductVO productVO, String userMessage);

    /**
     * Place product order.
     */
    String orderProduct(ProductVO productVO);
}
