package com.smartLive.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.common.core.enums.product.ItemActionType;
import com.smartLive.product.domain.Product;
import com.smartLive.product.domain.VO.ProductVO;

import java.util.List;
import java.util.Map;

public interface IProductService extends IService<Product> {

    ProductVO selectProductById(Long id);

    Product selectProductEntityById(Long id);

    List<Product> selectProductEntityList(Product product);

    List<ProductVO> selectProductList(Product product);

    int insertProduct(Product product);

    int updateProduct(Product product);

    int deleteProductById(Long id);

    int deleteProductByIds(Long[] ids);

    int addStock(Long id);

    Boolean changeStatus(Product product);

    int priceReduced(Long id);

    Long purchaseProduct(Long productId, Long userId);

    List<ProductVO> queryProductOfShop(Product product);

    List<Product> listProduct();

    Integer getProductTotal();

    List<Product> getProductListByIds(List<Long> sourceIdList);

    List<Product> getShopSlowProducts(Long shopId, Integer limit);

    ProductVO getProductById(Long id);

    List<ProductVO> getHotProductRank(Integer current, Integer size, Integer category);

    List<ProductVO> searchProducts(String keyword);

    String allPublish();

    String publish(String[] ids);

    Boolean updateReviewCountBatch(Map<Long, Integer> updateMap);

    Boolean updateStarCountBatch(Map<Long, Integer> updateMap);

    Boolean updateFansCountBatch(Map<Long, Integer> updateMap);

    Boolean updateSoldBatch(Map<Long, Integer> updateMap);

    void syncSalesData();

    Integer getProductStarCount(Long sourceId);

    Boolean updateProductStatus(Long id, Integer status, String reason);

    boolean deductStock(Long id);

    boolean recoverStock(Long id, Long userId);

    boolean recoverRedisStockAndEligibility(Long productId, Long userId);

    void sendProductActionMessageToMQ(Long productId, ItemActionType itemActionType);
}
