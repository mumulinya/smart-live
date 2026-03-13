package com.smartLive.product.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.common.core.enums.product.ItemActionType;
import com.smartLive.product.domain.VO.ProductVO;
import com.smartLive.product.domain.Product;

/**
 * 商品业务契约接口
 * 定义商品、代金券、团购套餐的核心生命周期管理及高并发购买行为
 * 
 * @author smartLive
 * @date 2026-02-18
 */
public interface IProductService extends IService<Product>
{
    /**
     * 查询商品
     * 
     * @param id 商品主键
     * @return 商品VO
     */
     ProductVO selectProductById(Long id);

    /**
     * 查询商品实体 (内部使用)
     *
     * @param id 商品主键
     * @return 商品
     */
     Product selectProductEntityById(Long id);

    /**
     * 查询商品列表 (实体)
     * 
     * @param product 商品
     * @return 商品集合
     */
     List<Product> selectProductEntityList(Product product);

    /**
     * 查询商品列表 (VO)
     *
     * @param product 商品
     * @return 商品VO集合
     */
     List<ProductVO> selectProductList(Product product);

    /**
     * 新增商品
     * 
     * @param product 商品
     * @return 结果
     */
     int insertProduct(Product product);

    /**
     * 修改商品
     * 
     * @param product 商品
     * @return 结果
     */
     int updateProduct(Product product);

    /**
     * 批量删除商品
     * 
     * @param ids 需要删除的商品主键集合
     * @return 结果
     */
     int deleteProductByIds(Long[] ids);

    /**
     * 删除商品信息
     * 
     * @param id 商品主键
     * @return 结果
     */
     int deleteProductById(Long id);

    /**
     * 根据店铺查询商品列表
     *
     * @param product 商品查询条件（包含shopId和category）
     * @return 商品VO列表
     */
    List<ProductVO> queryProductOfShop(Product product);

    /**
     * 购买商品（统一入口，内部策略分发）
     *
     * @param productId 商品ID
     * @param userId    用户ID
     * @return 订单ID
     */
    Long purchaseProduct(Long productId, Long userId);

    /**
     * 查询全部商品列表（含内部店铺关联信息）
     * 用于管理员或离线统计场景
     *
     * @return 商品原始实体列表
     */
    List<Product> listProduct();

    /**
     * 手动触发全量发布
     * 将库中所有商品同步至 Elasticsearch 搜索索引和 Milvus 向量索引
     *
     * @return 发布任务的简要执行结果
     */
    String allPublish();

    /**
     * 批量发布商品至ES和Milvus索引
     *
     * @param ids 商品ID数组
     * @return 发布结果
     */
    String publish(String[] ids);

    /**
     * 获取商品总数
     *
     * @return 商品总数
     */
    Integer getProductTotal();

    /**
     * 获取商品列表
     *
     * @param sourceIdList 商品id列表
     * @return 商品列表
     */
    List<Product> getProductListByIds(List<Long> sourceIdList);

    /**
     * 获取商品VO
     *
     * @param id 商品id
     * @return 商品VO
     */
    ProductVO getProductById(Long id);

    /**
     * 添加库存 (仅用于秒杀恢复或其他业务?) -> Actually logic might be different now.
     * Keeping signature similar but updated to Product.
     *
     * @param id 商品id
     * @return 添加结果
     */
    int addStock(Long id);

    /**
     * 修改商品状态
     *
     * @param product 商品
     * @return 修改结果
     */
    Boolean changeStatus(Product product);

    /**
     * 商品降价通知
     *
     * @param id 商品id
     * @return 结果
     */
    int priceReduced(Long id);

    /**
     * 批量更新评价数
     *
     * @param updateMap 批量更新评价数
     * @return 结果
     */
    Boolean updateReviewCountBatch(Map<Long, Integer> updateMap);

    /**
     * 批量更新商品收藏数
     *
     * @param updateMap 商铺id和收藏数
     * @return 更新结果
     */
    Boolean updateStarCountBatch(Map<Long, Integer> updateMap);

    /**
     * Batch update product fans count.
     *
     * @param updateMap productId -> fansCount
     * @return update result
     */
    Boolean updateFansCountBatch(Map<Long, Integer> updateMap);

    /**
     * 获取商品收藏数
     *
     * @param sourceId 商品id
     * @return 收藏数
     */
    Integer getProductStarCount(Long sourceId);

    /**
     * 更新商品审批状态并处理关联逻辑
     * 如审核通过则自动触发索引同步与上架 MQ 通知
     *
     * @param id     商品 ID
     * @param status 商品状态 (NORMAL, OFF_SHELF, AUDIT_FAIL 等)
     * @param reason 审核失败原因（可选）
     * @return 更新是否成功
     */
    Boolean updateProductStatus(Long id, Integer status, String reason);

    /**
     * 获取热门商品排行榜
     *
     * @param current  页码
     * @param size     每页数量
     * @param category 种类 (1:代金券, 2:团购套餐)
     * @return 热门商品列表
     */
    List<ProductVO> getHotProductRank(Integer current, Integer size, Integer category);

    /**
     * 扣减库存
     * @param id 商品id
     * @return 结果
     */
    boolean deductStock(Long id);

    /**
     * 恢复数据库库存
     * @param id 商品id
     * @return 结果
     */
    boolean recoverStock(Long id, Long userId);

    /**
     * 恢复 Redis 中的秒杀库存及用户购买资格
     *
     * @param productId 商品ID
     * @param userId    用户ID
     * @return 恢复结果
     */
    boolean recoverRedisStockAndEligibility(Long productId, Long userId);

    /**
     * 发送商品动态操作 MQ 消息（降价/重新上架/即将下架等）
     *
     * @param productId      商品ID
     * @param itemActionType 动作类型
     */
    void sendProductActionMessageToMQ(Long productId, ItemActionType itemActionType);

    /**
     * 批量更新销量
     * @param updateMap 商品id和销量
     * @return 结果
     */
    Boolean updateSoldBatch(Map<Long, Integer> updateMap);

    /**
     * 同步销量数据从 Redis 到数据库
     */
    void syncSalesData();
}
