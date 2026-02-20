package com.smartLive.product.service.impl;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.*;
import com.smartLive.common.core.enums.FeedTypeEnum;
import com.smartLive.common.core.enums.ItemActionType;
import com.smartLive.common.rabbitmq.domain.AuditMessage;
import com.smartLive.common.rabbitmq.domain.FeedEventMessage;
import com.smartLive.common.rabbitmq.domain.ContentSyncMessage;
import com.smartLive.common.rabbitmq.domain.ContentBatchSyncMessage;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.common.redis.util.CacheClient;
import com.smartLive.interaction.api.RemoteFollowService;
import com.smartLive.interaction.api.RemoteStarService;
import com.smartLive.interaction.api.DTO.FollowDTO;
import com.smartLive.interaction.api.DTO.StarDTO;
import com.smartLive.product.domain.VO.ProductVO;
import com.smartLive.product.service.strategy.PurchaseStrategy;
import com.smartLive.common.redis.util.RedisBatchCacheUtil;
import java.util.concurrent.TimeUnit;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.DTO.ShopDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.smartLive.product.mapper.ProductMapper;
import com.smartLive.product.domain.Product;
import com.smartLive.product.service.IProductService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品Service业务层处理
 *
 * @author 桃桃
 * @date 2026-02-18
 */
@Service
@Slf4j
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService
{
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private RedisService redisService;
    @Autowired
    private RemoteShopService remoteShopService;
    @Autowired
    private RemoteStarService remoteStarService;
    @Autowired
    private RemoteFollowService remoteFollowService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    private Map<String, PurchaseStrategy> purchaseStrategyMap;
    @Autowired
    private RedisBatchCacheUtil redisBatchCacheUtil;
    @Autowired
    private CacheClient cacheClient;

    /**
     * 查询商品
     *
     * @param id 商品主键
     * @return 商品VO
     */
    @Override
    public ProductVO selectProductById(Long id)
    {
        Product product = productMapper.selectProductById(id);
        if (product != null){
            queryProductShopMessage(product);
             // View count / interactions check could be here
        }
        return convertToProductVO(product);
    }

    @Override
    public Product selectProductEntityById(Long id)
    {
        Product product = productMapper.selectProductById(id);
        if (product != null){
            queryProductShopMessage(product);
        }
        return product;
    }

    /**
     * 查询商品列表 (实体)
     *
     * @param product 商品
     * @return 商品集合
     */
    @Override
    public List<Product> selectProductEntityList(Product product)
    {
        List<Product> productList = productMapper.selectProductList(product);
        // Previously querySeckill was called here, but now fields are merged.
        return productList;
    }


    @Override
    public List<ProductVO> selectProductList(Product product)
    {
        List<Product> productList = selectProductEntityList(product);
        return convertToProductVOList(productList);
    }

    /**
     * 新增商品
     *
     * @param product 商品
     * @return 结果
     */
    @Override
    @Transactional
    public int insertProduct(Product product)
    {
        product.setCreateTime(DateUtils.getNowDate());
        
        // 保存商品
        int i = productMapper.insertProduct(product);
        if(i > 0){
            // 如果是秒杀商品，初始化Redis库存
            if (product.getActivityType() != null && product.getActivityType() == 1) {
                redisService.setCacheObject(RedisConstants.SECKILL_STOCK_KEY + product.getId(), product.getStock());
            }

            // 发送消息推送动态
            sendNewProductMessageToMQ(product);
            // 发送审核消息
            sendAuditMessage(product);
        }
        return i;
    }

    /**
     * 店铺发布新商品，更新用户动态
     * @param product
     */
    public void sendNewProductMessageToMQ(Product product){
        FeedEventMessage feedEventMessage= FeedEventMessage
                .builder()
                .feedType(FeedTypeEnum.SHOP_FEED.getCode())
                .sourceType(GlobalBizTypeEnum.SHOP.getCode())
                .sourceId(product.getShopId())
                .bizType(GlobalBizTypeEnum.PRODUCT.getCode()) // KEEP VOUCHER TYPE FOR NOW
                .bizId(product.getId())
                .publishTime(DateUtils.getNowDate())
                .action(ItemActionType.NEW_ITEM.getCode())
                .build();
        MqMessageSendUtils.sendMqMessage(rabbitTemplate,
                MqConstants.INTERACT_FEED_EXCHANGE_NAME,
                MqConstants.INTERACT_FEED_ROUTING,
                feedEventMessage);
    }
    /**
     * 商品动态操作
     * @param productId
     * @param itemActionType
     */
    public void sendProductActionMessageToMQ(Long productId, ItemActionType itemActionType){
        FeedEventMessage feedEventMessage= FeedEventMessage
                .builder()
                .feedType(FeedTypeEnum.ITEM_FEED.getCode())
                .sourceType(GlobalBizTypeEnum.PRODUCT.getCode()) // KEEP VOUCHER
                .sourceId(productId)
                .bizType(GlobalBizTypeEnum.PRODUCT.getCode()) // KEEP VOUCHER
                .bizId(productId)
                .publishTime(DateUtils.getNowDate())
                .action(itemActionType.getCode())
                .build();
        MqMessageSendUtils.sendMqMessage(rabbitTemplate,
                MqConstants.INTERACT_FEED_EXCHANGE_NAME,
                MqConstants.INTERACT_FEED_ROUTING,
                feedEventMessage);
    }

    /**
     * 修改商品
     *
     * @param product 商品
     * @return 结果
     */
    @Override
    public int updateProduct(Product product)
    {
        product.setUpdateTime(DateUtils.getNowDate());
        int i = productMapper.updateProduct(product);
        if(i > 0){
            // 发送审核消息
            sendAuditMessage(product);
            
            // 如果是秒杀，更新Redis库存
            if(product.getActivityType() != null && product.getActivityType() == 1){
                redisService.setCacheObject(RedisConstants.SECKILL_STOCK_KEY + product.getId(), product.getStock());
            }

            // 更新ES数据
            publish(new String[]{product.getId().toString()});
            clearProductCache(product.getId());
        }
        return i;
    }

    /**
     * 修改商品状态
     *
     * @param product 商品
     * @return 修改结果
     */
    @Override
    public Boolean changeStatus(Product product) {
        boolean b = updateById(product);
        if (b){
            // 更新ES数据
            publish(new String[]{product.getId().toString()});
            clearProductCache(product.getId());
            if (product.getStatus() == 1){
                // 上架，发送mq消息更新用户动态
                sendProductActionMessageToMQ(product.getId(), ItemActionType.RESHELF);
            }
        }
        return b;
    }

    /**
     * 商品降价
     *
     * @param id 商品id
     * @return 结果
     */
    @Override
    public int priceReduced(Long id) {
        sendProductActionMessageToMQ(id, ItemActionType.PRICE_DROP);
        return 1;
    }

    /**
     * 批量删除商品
     *
     * @param ids 需要删除的商品主键
     * @return 结果
     */
    @Override
    public int deleteProductByIds(Long[] ids)
    {
        int i = productMapper.deleteProductByIds(ids);
        // 删除es数据
        if (i > 0) {
            clearProductCacheBatch(Arrays.asList(ids));
            for (Long id : ids) {
                executorService.submit(()->{
                    log.info("线程{}删除es数据id为：{}", Thread.currentThread().getName(), id);
                    ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
                    contentSyncMessage.setId(id);
                    contentSyncMessage.setIndexName(EsIndexNameConstants.PRODUCT_INDEX_NAME); // KEEP VOUCHER INDEX
                    contentSyncMessage.setType(GlobalBizTypeEnum.PRODUCT.getCode()); // KEEP VOUCHER
                    // 发起rabbitMq信息删除es数据
                    MqMessageSendUtils.sendMqMessage(rabbitTemplate, MqConstants.ES_EXCHANGE, MqConstants.ES_ROUTING_DELETE, contentSyncMessage);
                    // 发起rabbitmq信息删除milvus数据
                    MqMessageSendUtils.sendMqMessage(rabbitTemplate, MqConstants.MILVUS_EXCHANGE, MqConstants.MILVUS_ROUTING_DELETE, contentSyncMessage);
                });
            }
        }
        return 1;
    }

    /**
     * 删除商品信息
     *
     * @param id 商品主键
     * @return 结果
     */
    @Override
    public int deleteProductById(Long id)
    {
        int rows = productMapper.deleteProductById(id);
        if (rows > 0) {
            clearProductCache(id);
        }
        return rows;
    }


    /**
     * 购买商品 (Strategy Pattern)
     */
    @Override
    @Transactional
    public Long purchaseProduct(Long productId, Long userId) {
        Product product = getById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        // Select strategy based on activityType
        // 0: Normal, 1: Seckill
        String strategyName = "NormalPurchaseStrategy";
        if (product.getActivityType() != null && product.getActivityType() == 1) {
            strategyName = "SeckillPurchaseStrategy";
        }
        
        PurchaseStrategy strategy = purchaseStrategyMap.get(strategyName);
        if (strategy == null) {
             throw new RuntimeException("未找到对应的购买策略: " + strategyName);
        }
        
        return strategy.purchase(userId, product);
    }

    /**
     * 根据店铺查询商品列表
     *
     * @param product
     * @return
     */
    @Override
    public List<ProductVO> queryProductOfShop(Product product) {
        List<Product> products = query()
                .eq("shop_id", product.getShopId())
                .eq("category", product.getCategory())
                .orderByAsc("create_time")
                .list();
        if (products.isEmpty()) {
            return null;
        }
        queryProductListShopMessage(products);
        return convertToProductVOList(products);
    }


    /**
     * 发送审核消息
     * @param product
     */
    private void sendAuditMessage(Product product) {
        AuditMessage auditMessage = AuditMessage.builder()
                .bizId(product.getId())
                .bizType(GlobalBizTypeEnum.PRODUCT.getCode()) // KEEP VOUCHER
                .submitterId(product.getShopId())
                .auditContent(BeanUtil.beanToMap(product))
                .createTime(product.getCreateTime())
                .build();
        MqMessageSendUtils.sendMqMessage(rabbitTemplate, MqConstants.AUDIT_EXCHANGE_NAME,MqConstants.AUDIT_ROUTING_KEY, auditMessage);
    }

    /**
     * 查询店铺的商品列表
     *
     * @return
     */
    @Override
    public List<Product> listProduct( ) {
        List<Product> list = query().list();
        list.forEach(this::queryProductShopMessage);
        return list;
    }

    /**
     * 查询商品店铺信息
     * @param product
     */
    void queryProductShopMessage(Product product){
        ShopDTO shopDTO = remoteShopService.getShopById(product.getShopId());
        if(shopDTO != null){
            product.setShopName(shopDTO.getName());
            product.setTypeId(shopDTO.getTypeId());
            product.setShopLogo(shopDTO.getShopLogo());
        }
    }

    private ProductVO convertToProductVO(Product product) {
        if (product == null) {
            return null;
        }
        ProductVO productVO = new ProductVO();
        BeanUtils.copyProperties(product, productVO);
        return productVO;
    }

    private List<ProductVO> convertToProductVOList(List<Product> productList) {
        if (productList == null || productList.isEmpty()) {
            return new ArrayList<>();
        }
        List<ProductVO> voList = new ArrayList<>(productList.size());
        for (Product product : productList) {
            voList.add(convertToProductVO(product));
        }
        return voList;
    }

    /**
     * 获取商品总数
     *
     * @return 商品总数
     */
    @Override
    public Integer getProductTotal() {
        return query().eq("activity_type", 0).count().intValue();
    }

    /**
     * 获取商品列表
     *
     * @param sourceIdList 商品id列表
     * @return 商品列表
     */
    @Override
    public List<Product> getProductListByIds(List<Long> sourceIdList) {
        List<Product> productList = redisBatchCacheUtil.queryBatchWithCache(
                RedisConstants.CACHE_PRODUCT_KEY,
                sourceIdList,
                Product.class,
                missingIds -> {
                    return lambdaQuery()
                            .in(Product::getId, missingIds)
                            .list();
                },
                Product::getId,
                RedisConstants.CACHE_PRODUCT_TTL,
                TimeUnit.MINUTES
        );
        queryProductListShopMessage(productList);
        return productList;
    }
    /**
     * 获取商品列表的店铺信息
     *
     * @param productList
     * @return 商品信息
     */
    private void queryProductListShopMessage(List<Product> productList) {
        // 获取所有店铺的 ID
        List<Long> distinctShopIds = productList.stream()
                .map(Product::getShopId)
                .distinct() // 核心：过滤掉重复的 shopId
                .toList();
        List<ShopDTO> shopList = remoteShopService.getShopList(distinctShopIds);
        if (CollectionUtils.isEmpty(shopList)) {
            log.error("店铺列表为空");
            return;
        }
        // 将 shopList 转换成 Map，Key: shopId，Value: ShopDTO
        Map<Long, ShopDTO> shopMap = shopList.stream()
                .collect(Collectors.toMap(
                        ShopDTO::getId,   // Key: 取店铺的 ID
                        shop -> shop,     // Value: 取店铺对象本身 (也可以写成 Function.identity())
                        (oldVal, newVal) -> oldVal // 兜底策略：如果万一有重复的 ID，保留第一个 (防止报错)
                ));
        if (!shopMap.isEmpty()) {
            productList.forEach(product -> {
                ShopDTO shopDTO = shopMap.get(product.getShopId());
                if (shopDTO != null) {
                    product.setShopName(shopDTO.getName());
                    product.setShopLogo(shopDTO.getShopLogo());
                }
            });
        }
    }

    /**
     * 获取商品
     *
     * @param id 商品id
     * @return 商品VO
     */
    @Override
    public ProductVO getProductById(Long id) {
        Product product = cacheClient.queryWithLogicalExpire(
                RedisConstants.CACHE_PRODUCT_KEY,
                id,
                Product.class,
                productMapper::selectProductById,
                RedisConstants.CACHE_PRODUCT_TTL,
                TimeUnit.MINUTES
        );
        if (product != null){
            // 判断是否收藏
            StarDTO starDTO=new StarDTO();
            starDTO.setSourceType(GlobalBizTypeEnum.PRODUCT.getCode()); // KEEP
            starDTO.setSourceId(id);
            Boolean isStar = remoteStarService.isStar(starDTO);
            product.setIsStar(isStar);
            // 判断是否关注
            FollowDTO followDTO=new FollowDTO();
            followDTO.setSourceType(GlobalBizTypeEnum.PRODUCT.getCode()); // KEEP
            followDTO.setSourceId(id);
            Boolean isFollow = remoteFollowService.isFollowed(followDTO);
            product.setIsFollow(isFollow);
        }
        return convertToProductVO(product);
    }

    /**
     * 添加库存 (通知)
     *
     * @param id 商品id
     * @return 添加结果
     */
    @Override
    public int addStock(Long id) {
        log.info("发送mq消息，更新用户动态");
        // 发送消息，更新用户动态
        sendProductActionMessageToMQ(id, ItemActionType.RESTOCK);
        return 1;
    }

    /**
     * 全部发布
     *
     * @return 全部发布结果
     */
    @Override
    public String allPublish() {
        int page = PageConstants.PAGE_NUMBER;
        int pageSize = PageConstants.ES_PAGE_SIZE; 
        while (true) {
            // 分页查询
            List<Product> products = query()
                    .page(new Page<>(page, pageSize))
                    .getRecords();
            if (products.isEmpty()) {
                break;
            }

            int finalPage = page;
            executorService.submit(()->{
                log.info("线程：{}开始发布的商品{}页", Thread.currentThread().getName(), finalPage);
                queryProductListShopMessage(products);
                // 发送批量消息
                sendProductBatchMessage(products);
                log.info("发送第 {} 页，{} 条数据", finalPage, products.size());
            });
            page++;
        }
        return "数据发布完成";
    }


    /**
     * 发布
     *
     * @param ids
     * @return 发布结果
     */
    @Override
    public String publish(String[] ids) {
        if (ids == null || ids.length == 0) {
            return "参数为空";
        }
        // Convert to Long list
        List<Long> idList = Arrays.stream(ids)
                .map(Long::valueOf)
                .collect(Collectors.toList());

        executorService.submit(() -> {
            log.info("线程{}，开始批量发布商品：{}", Thread.currentThread().getName(), idList);
            // Batch query
            List<Product> products = query().in("id", idList).list();
            if (CollUtil.isNotEmpty(products)) {
                // Batch populate shop info
                queryProductListShopMessage(products);
                // Batch send message
                sendProductBatchMessage(products);
            }
        });
        return "发布成功";
    }

    /**
     * 批量发送ES和Milvus同步消息
     * @param products
     */
    private void sendProductBatchMessage(List<Product> products) {
        if (CollUtil.isEmpty(products)) {
            return;
        }
        ContentBatchSyncMessage request = new ContentBatchSyncMessage();
        request.setIndexName(EsIndexNameConstants.PRODUCT_INDEX_NAME);
        request.setData(products);
        request.setType(GlobalBizTypeEnum.PRODUCT.getCode());
        
        // 发送rabbitmq消息数据插入es
        MqMessageSendUtils.sendMqMessage(rabbitTemplate, MqConstants.ES_EXCHANGE, MqConstants.ES_ROUTING_BATCH_INSERT, request);
        // 发送rabbitmq消息数据插入Milvus
        MqMessageSendUtils.sendMqMessage(rabbitTemplate, MqConstants.MILVUS_EXCHANGE, MqConstants.MILVUS_ROUTING_BATCH_INSERT, request);
    }

    private void clearProductCache(Long productId) {
        if (productId == null) {
            return;
        }
        redisService.deleteObject(RedisConstants.CACHE_PRODUCT_KEY + productId);
        redisService.deleteObject(RedisConstants.SECKILL_STOCK_KEY + productId);
    }

    private void clearProductCacheBatch(Collection<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return;
        }
        List<String> productKeys = productIds.stream()
                .filter(Objects::nonNull)
                .map(id -> RedisConstants.CACHE_PRODUCT_KEY + id)
                .toList();
        if (CollUtil.isNotEmpty(productKeys)) {
            redisService.deleteObject(productKeys);
        }
        List<String> seckillKeys = productIds.stream()
                .filter(Objects::nonNull)
                .map(id -> RedisConstants.SECKILL_STOCK_KEY + id)
                .toList();
        if (CollUtil.isNotEmpty(seckillKeys)) {
            redisService.deleteObject(seckillKeys);
        }
    }
    /**
     * 批量更新商品收藏数量
     *
     * @param updateMap
     * @return
     */
    @Override
    public Boolean updateReviewCountBatch(Map<Long, Integer> updateMap) {
        if (CollUtil.isEmpty(updateMap)) {
            return false;
        }
        if (updateMap.size() > 500) {
            List<List<Long>> partition = ListUtil.partition(new ArrayList<>(updateMap.keySet()), 500);
            for (List<Long> batchKeys : partition) {
                Map<Long, Integer> batchMap = new HashMap<>();
                for (Long key : batchKeys) {
                    batchMap.put(key, updateMap.get(key));
                }
                productMapper.updateReviewCountBatch(batchMap);
            }
        } else {
            productMapper.updateReviewCountBatch(updateMap);
        }
        clearProductCacheBatch(updateMap.keySet());
        return true;
    }
    /**
     * 获取商品收藏数量
     *
     * @param
     * @return
     */
    @Override
    public Boolean updateStarCountBatch(Map<Long, Integer> updateMap) {
        if (CollUtil.isEmpty(updateMap)) {
            return false;
        }
        if (updateMap.size() > 500) {
            List<List<Long>> partition = ListUtil.partition(new ArrayList<>(updateMap.keySet()), 500);
            for (List<Long> batchKeys : partition) {
                Map<Long, Integer> batchMap = new HashMap<>();
                for (Long key : batchKeys) {
                    batchMap.put(key, updateMap.get(key));
                }
                productMapper.updateStarCountBatch(batchMap);
            }
        } else {
            productMapper.updateStarCountBatch(updateMap);
        }
        clearProductCacheBatch(updateMap.keySet());
        return true;
    }
    /**
     * 获取商品收藏数量
     *
     * @param sourceId
     * @return
     */
    @Override
    public Integer getProductStarCount(Long sourceId) {
        Product product = lambdaQuery()
                .select(Product::getStars)
                .eq(Product::getId, sourceId)
                .one();
        return product != null ? product.getStars() : 0;
    }
    /**
     * 修改商品状态
     *
     * @param id
     * @param status
     * @return
     */
    @Override
    public Boolean updateProductStatus(Long id, Integer status) {
        boolean b = update(new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Product>().set("status", status).eq("id", id));
        if(b){
            publish(new String[]{id.toString()});
            clearProductCache(id);
        }
        return b;
    }
    /**
     * 扣减库存
     *
     * @param id
     * @return
     */
    @Override
    public boolean deductStock(Long id) {
        // Simple update: stock = stock - 1 where id = id and stock > 0
        return update().setSql("stock = stock - 1").eq("id", id).gt("stock", 0).update();
    }
    /**
     * 恢复库存
     *
     * @param id
     * @return
     */
    @Override
    public boolean recoverStock(Long id) {
        // Simple update: stock = stock + 1 where id = id
        return update().setSql("stock = stock + 1").eq("id", id).update();
    }

}
