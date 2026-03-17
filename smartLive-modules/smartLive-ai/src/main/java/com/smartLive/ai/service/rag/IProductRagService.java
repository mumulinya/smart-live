package com.smartLive.ai.service.rag;

import com.smartLive.ai.entity.vo.ProductVO;

import java.util.List;

/**
 * 商品 RAG 服务接口。
 */
public interface IProductRagService {

    /**
     * 查询商品列表。
     */
    List<ProductVO> getProductList(ProductVO productVO, String userMessage);

    /**
     * 下单商品。
     */
    String orderProduct(ProductVO productVO);
}
