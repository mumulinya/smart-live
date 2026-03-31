package com.smartLive.product.service.impl;
import com.smartLive.common.core.constant.mq.InteractionMqConstants;
import com.smartLive.common.core.constant.mq.SearchMqConstants;
import com.smartLive.common.core.constant.mq.AiAuditMqConstants;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.*;
import com.smartLive.common.core.enums.common.AuditStatusEnum;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.interaction.FeedTypeEnum;
import com.smartLive.common.core.enums.product.ItemActionType;
import com.smartLive.common.core.enums.product.ProductEnum;
import com.smartLive.common.core.enums.product.ProductStatusEnum;
import com.smartLive.common.core.enums.product.SalesTypeEnum;
import com.smartLive.common.rabbitmq.domain.AuditMessage;
import com.smartLive.common.rabbitmq.domain.FeedEventMessage;
import com.smartLive.common.rabbitmq.domain.ContentSyncMessage;
import com.smartLive.common.rabbitmq.domain.ContentBatchSyncMessage;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.security.utils.SecurityUtils;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.common.redis.util.CacheClient;
import com.smartLive.common.redis.util.RedisMultiCacheManager;
import com.smartLive.common.redis.util.ZSetIdManager;
import com.smartLive.interaction.api.DTO.StarDTO;
import com.smartLive.interaction.api.RemoteFollowService;
import com.smartLive.interaction.api.RemoteStarService;
import com.smartLive.interaction.api.DTO.FollowDTO;

import com.smartLive.product.domain.VO.ProductVO;
import com.smartLive.product.service.strategy.PurchaseStrategy;

import java.util.concurrent.TimeUnit;

import com.smartLive.system.api.RemoteUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.smartLive.product.mapper.ProductMapper;
import com.smartLive.product.domain.Product;
import com.smartLive.product.service.IProductService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品模块服务实现类
 *
 * 处理商品链路的核心业务逻辑，包含：商品上下架、审核对接、缓存处理、MQ事件流发送、ES/Milvus同步等。
 *
 * @author smartLive
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
    private RemoteStarService remoteStarService;
    @Autowired
    private RemoteFollowService remoteFollowService;
    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    private MqMessageSendUtils mqMessageSendUtils;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    private Map<String, PurchaseStrategy> purchaseStrategyMap;
    @Autowired
    private RedisMultiCacheManager redisMultiCacheManager;
    @Autowired
    private CacheClient cacheClient;
    @Autowired
    private ZSetIdManager zSetIdManager;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 根据ID查询商品VO信息
     *
     * @param id 商品ID
     * @return 商品VO
     */
    @Override
    public ProductVO selectProductById(Long id)
    {
        Product product = productMapper.selectProductById(id);
        return convertToProductVO(product);
    }

    /**
     * 根据ID查询商品实体
     *
     * @param id 商品ID
     * @return 商品实体
     */
    @Override
    public Product selectProductEntityById(Long id)
    {
        Product product = productMapper.selectProductById(id);
        return product;
    }

    /**
     * 查询商品实体列表
     *
     * @param product 商品查询条件
     * @return 商品实体列表
     */
    @Override
    public List<Product> selectProductEntityList(Product product)
    {
        if (product == null)
        {
            product = new Product();
        }
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId != null && !SecurityUtils.isAdmin(currentUserId))
        {
            List<Long> shopIds = remoteUserService.getShopIdsByUserId(currentUserId);
            if (CollUtil.isEmpty(shopIds))
            {
                return Collections.emptyList();
            }
            product.setShopIds(shopIds);
        }
        List<Product> productList = productMapper.selectProductList(product);
        // Previously querySeckill was called here, but now fields are merged.
        return productList;
    }


    /**
     * 查询商品实体列表
     *
     * @param product 商品查询条件
     * @return 商品实体列表
     */
    @Override
    public List<ProductVO> selectProductList(Product product)
    {
        List<Product> productList = selectProductEntityList(product);
        return convertToProductVOList(productList);
    }

    /**
     * 新增商品
     *
     * @param product 商品信息
     * @return 影响行数
     */
    @Override
    @Transactional
    public int insertProduct(Product product)
    {
        product.setCreateTime(DateUtils.getNowDate());

        // 插入商品记录及执行落库操作
        int i = productMapper.insertProduct(product);
        if(i > 0){
            // 同步执行下游链路数据补偿保障
            sendAuditMessage(product);
        }
        return i;
    }

    /**
     * 发送新商品Feed流消息到MQ
     *
     * @param product 商品实体
     */
    public void sendNewProductMessageToMQ(Product product){
        String[] shopIds = product.getShopId().split(",");
        for(String shopId : shopIds){
            FeedEventMessage feedEventMessage= FeedEventMessage
                    .builder()
                    .feedType(FeedTypeEnum.SHOP_FEED.getCode())
                    .sourceType(GlobalBizTypeEnum.SHOP.getCode())
                    .sourceId(Long.valueOf(shopId))
                    .bizType(GlobalBizTypeEnum.PRODUCT.getCode()) // KEEP VOUCHER TYPE FOR NOW
                    .bizId(product.getId())
                    .publishTime(DateUtils.getNowDate())
                    .action(ItemActionType.NEW_ITEM.getCode())
                    .build();
            mqMessageSendUtils.sendMqMessage(
                    InteractionMqConstants.INTERACT_FEED_EXCHANGE,
                    InteractionMqConstants.INTERACT_FEED_ROUTING_KEY,
                    feedEventMessage);
        }
    }
    /**
     * 发送商品动作消息到MQ
     *
     * @param productId 商品ID
     * @param itemActionType 动作类型
     */
    @Override
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
        mqMessageSendUtils.sendMqMessage(
                InteractionMqConstants.INTERACT_FEED_EXCHANGE,
                InteractionMqConstants.INTERACT_FEED_ROUTING_KEY,
                feedEventMessage);
    }

    /**
     * 更新商品信息
     *
     * @param product 商品信息
     * @return 影响行数
     */
    @Override
    public int updateProduct(Product product)
    {
        product.setUpdateTime(DateUtils.getNowDate());
        int i = productMapper.updateProduct(product);
        if(i > 0){
            // 同步执行下游链路数据补偿保障
            sendAuditMessage(product);

            // 插入商品记录及执行落库操作
            if(product.getActivityType() != null && product.getActivityType() == 1){
                redisService.deleteObject(RedisConstants.SECKILL_STOCK_KEY + product.getId());
                // 执行业务功能联动及状态补偿保障机制
                redisService.setCacheObject(RedisConstants.SECKILL_STOCK_KEY + product.getId(), product.getStock());
            }

            // 执行业务功能联动及状态补偿保障机制
            publish(new String[]{product.getId().toString()});
            clearProductCache(product.getId());
        }
        return i;
    }

    /**
     * 发送商品动作消息到MQ
     *
     * @param productId 商品ID
     * @param itemActionType 动作类型
     */
    @Override
    public Boolean changeStatus(Product product) {
        boolean b = updateById(product);
        if (b){
            // 执行业务功能联动及状态补偿保障机制
            publish(new String[]{product.getId().toString()});
            clearProductCache(product.getId());
            if (product.getStatus() == 1){
                // 执行业务功能联动及状态补偿保障机制
                sendProductActionMessageToMQ(product.getId(), ItemActionType.RESHELF);
            }
        }
        return b;
    }

    /**
     * 发送商品动作消息到MQ
     *
     * @param productId 商品ID
     * @param itemActionType 动作类型
     */
    @Override
    public int priceReduced(Long id) {
        sendProductActionMessageToMQ(id, ItemActionType.PRICE_DROP);
        return 1;
    }

    /**
     * 批量删除商品
     *
     * @param ids 商品ID数组
     * @return 影响行数
     */
    @Override
    public int deleteProductByIds(Long[] ids)
    {
        int i = productMapper.deleteProductByIds(ids);
        // 同步执行下游链路数据补偿保障
        if (i > 0) {
            List<Long> productIds = Arrays.asList(ids);
            clearProductCacheBatch(productIds);
            List<String> seckillKeys = productIds.stream()
                    .filter(Objects::nonNull)
                    .map(id -> RedisConstants.SECKILL_STOCK_KEY + id)
                    .toList();
            if (CollUtil.isNotEmpty(seckillKeys)) {
                redisService.deleteObject(seckillKeys);
            }
            for (Long id : ids) {
                executorService.submit(()->{
                    log.info("Deleting product {} from search indexes on thread {}", id, Thread.currentThread().getName());
                    ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
                    contentSyncMessage.setId(id);
                    contentSyncMessage.setIndexName(EsIndexNameConstants.PRODUCT_INDEX_NAME); // KEEP VOUCHER INDEX
                    contentSyncMessage.setType(GlobalBizTypeEnum.PRODUCT.getCode()); // KEEP VOUCHER
                    // 同步执行下游链路数据补偿保障
                    mqMessageSendUtils.sendMqMessage( SearchMqConstants.ES_SYNC_EXCHANGE, SearchMqConstants.ES_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
                    // 同步执行下游链路数据补偿保障
                    mqMessageSendUtils.sendMqMessage( SearchMqConstants.MILVUS_SYNC_EXCHANGE, SearchMqConstants.MILVUS_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
                });
            }
        }
        return 1;
    }

    /**
     * 单条删除商品
     *
     * @param id 商品ID
     * @return 影响行数
     */
    @Override
    public int deleteProductById(Long id)
    {
        int rows = productMapper.deleteProductById(id);
        if (rows > 0) {
            clearProductCache(id);
            redisService.deleteObject(RedisConstants.SECKILL_STOCK_KEY + id);
        }
        return rows;
    }


    /**
     * 购买商品（策略模式）
     *
     * @param productId 商品ID
     * @param userId 用户ID
     * @return 订单ID
     */
    @Override
    @Transactional
    public Long purchaseProduct(Long productId, Long userId) {
        Product product = getById(productId);
        if (product == null) {
            throw new RuntimeException("product not found");
        }
        // 执行业务功能联动及状态补偿保障机制
        String strategyName = "NormalPurchaseStrategy";
        if (product.getActivityType() != null && product.getActivityType() == 1) {
            strategyName = "SeckillPurchaseStrategy";
        }

        PurchaseStrategy strategy = purchaseStrategyMap.get(strategyName);
        if (strategy == null) {
             throw new RuntimeException("未找到对应的商品购买策略: " + strategyName);
        }

        return strategy.purchase(userId, product);
    }

    /**
     * 查询店铺关联商品列表
     *
     * @param product 查询条件
     * @return 商品VO列表
     */
    @Override
    public List<ProductVO> queryProductOfShop(Product product) {
        List<Product> products = query()
                .apply("FIND_IN_SET({0}, shop_id)", product.getShopId())
                .eq("category", product.getCategory())
                .eq("status", ProductStatusEnum.ON_SHELF.getCode())
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .orderByAsc("create_time")
                .list();
        if (products.isEmpty()) {
            return null;
        }
        return convertToProductVOList(products);
    }


    /**
     * 发送商品审核消息
     *
     * @param product 商品信息
     */
    private void sendAuditMessage(Product product) {
        AuditMessage auditMessage = AuditMessage.builder()
                .bizId(product.getId())
                .bizType(GlobalBizTypeEnum.PRODUCT.getCode()) // KEEP VOUCHER
                .submitterId(Long.valueOf(product.getShopId().split(",")[0]))
                .auditContent(BeanUtil.beanToMap(product))
                .createTime(product.getCreateTime())
                .build();
        mqMessageSendUtils.sendMqMessage( AiAuditMqConstants.AUDIT_DIRECT_EXCHANGE,AiAuditMqConstants.AUDIT_ROUTING_KEY, auditMessage);
    }

    /**
     * 获取全量商品信息
     *
     * @return 商品列表
     */
    @Override
    public List<Product> listProduct( ) {
        return query().list();
    }

    /**
     * 实体转VO
     *
     * @param product 实体
     * @return VO
     */
    private ProductVO convertToProductVO(Product product) {
        if (product == null) {
            return null;
        }
        ProductVO productVO = new ProductVO();
        BeanUtils.copyProperties(product, productVO);
        return productVO;
    }

    /**
     * 实体列表转VO列表
     *
     * @param productList 实体列表
     * @return VO列表
     */
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
     * 批量获取商品列表
     *
     * @param sourceIdList ID列表
     * @return 实体列表
     */
    @Override
    public List<Product> getProductListByIds(List<Long> sourceIdList) {
        List<Product> productList = redisMultiCacheManager.queryBatchWithCache(
                RedisConstants.CACHE_PRODUCT_KEY,
                RedisConstants.LOCK_PRODUCT_KEY,
                sourceIdList,
                Product.class,
                missingIds -> lambdaQuery()
                        .in(Product::getId, missingIds)
                        .eq(Product::getStatus, ProductStatusEnum.ON_SHELF.getCode())
                        .eq(Product::getAuditStatus, AuditStatusEnum.PASS.getCode())
                        .list(),
                Product::getId,
                RedisConstants.CACHE_PRODUCT_TTL,
                TimeUnit.MINUTES
        );
        if (CollUtil.isEmpty(productList)) {
            return Collections.emptyList();
        }
        return productList.stream()
                .filter(product -> product != null
                        && Objects.equals(product.getStatus(), ProductStatusEnum.ON_SHELF.getCode())
                        && Objects.equals(product.getAuditStatus(), AuditStatusEnum.PASS.getCode()))
                .collect(Collectors.toList());
    }


    /**
     * 实体转VO
     *
     * @param product 实体
     * @return VO
     */
    @Override
    public List<Product> getShopSlowProducts(Long shopId, Integer limit) {
        if (shopId == null) {
            return Collections.emptyList();
        }
        int safeLimit = limit == null || limit <= 0 ? 3 : Math.min(limit, 10);
        return query()
                .select("id", "name", "sold")
                .apply("FIND_IN_SET({0}, shop_id)", shopId)
                .eq("status", ProductStatusEnum.ON_SHELF.getCode())
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .orderByAsc("sold")
                .orderByAsc("id")
                .last("limit " + safeLimit)
                .list();
    }

    @Override
    public ProductVO getProductById(Long id) {
        Product product = cacheClient.queryWithLogicalExpireAndPassThrough(
                RedisConstants.CACHE_PRODUCT_KEY,
                RedisConstants.LOCK_PRODUCT_KEY,
                id,
                Product.class,
                productId -> query()
                        .eq("id", productId)
                        .eq("status", ProductStatusEnum.ON_SHELF.getCode())
                        .eq("audit_status", AuditStatusEnum.PASS.getCode())
                        .one(),
                RedisConstants.CACHE_PRODUCT_TTL,
                TimeUnit.MINUTES
        );
        if (product == null){
            return null;
        }
        if (!Objects.equals(product.getStatus(), ProductStatusEnum.ON_SHELF.getCode())
                || !Objects.equals(product.getAuditStatus(), AuditStatusEnum.PASS.getCode())) {
            return null;
        }
        ProductVO productVO = convertToProductVO(product);
        // 同步执行下游链路数据补偿保障
        StarDTO starDTO=new StarDTO();
        starDTO.setSourceType(GlobalBizTypeEnum.PRODUCT.getCode()); // KEEP
        starDTO.setSourceId(id);
        Boolean isStar = remoteStarService.isStar(starDTO);
        productVO.setIsStar(isStar);
        // 同步执行下游链路数据补偿保障
        FollowDTO followDTO=new FollowDTO();
        followDTO.setSourceType(GlobalBizTypeEnum.PRODUCT.getCode()); // KEEP
        followDTO.setSourceId(id);
        Boolean isFollow = remoteFollowService.isFollowed(followDTO);
        productVO.setIsFollow(isFollow);
        return productVO;
    }

    /**
     * 实体列表转VO列表
     *
     * @param productList 实体列表
     * @return VO列表
     */
    @Override
    public List<ProductVO> getHotProductRank(Integer current, Integer size, Integer category) {
        int pageNo = current == null || current < 1 ? 1 : current;
        int pageSize = size == null || size < 1 ? 10 : size;

        // 1. 分页参数校验：限制热门榜最多只能查询前100名的数据，防止深度分页
        if (pageNo * pageSize > 100) {
            return Collections.emptyList();
        }

        // 2. 根据枚举组装对应的热门 ZSet 缓存 Key
        String hotRankKey = RedisConstants.PRODUCT_HOT_RANK_KEY;
        if (ProductEnum.VOUCHER.getCode().equals(category)) {
            hotRankKey += "voucher";
        } else if (ProductEnum.SET_MEAL.getCode().equals(category)) {
            hotRankKey += "deal";
        } else {
            return Collections.emptyList();
        }

        List<ProductVO> resultList;

        // 3. 从 ZSet 根据分数分页倒序取出商品 ID
        Page<Long> longPage = zSetIdManager.pageIds(hotRankKey, null, current, SystemConstants.MAX_PAGE_SIZE);
        List<Long> productIdList = longPage.getRecords();

        if (CollUtil.isEmpty(productIdList)&&current == 1) {
            // 缓存未命中时触发防穿透补偿：DB全量加载并截取顶层数据重建ZSet
            log.info("Product hot rank cache miss for key {}, loading from database", hotRankKey);
            List<Product> dbList = query()
                    .eq("category", category)
                    .eq("status", ProductStatusEnum.ON_SHELF.getCode())
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .orderByDesc("sold")
                    .orderByDesc("create_time")
                    .list();

            if (CollUtil.isEmpty(dbList)) return Collections.emptyList();

            // 限制热门榜最多维护 100 条头部缓存
            List<Product> finalDbList = dbList.stream().limit(100).collect(Collectors.toList());
            final String finalHotRankKey = hotRankKey;
            // 开启异步线程重建 ZSet 并抛入定时重构队列
            executorService.execute(() -> {
                try {
                    zSetIdManager.saveToZSet(finalHotRankKey, finalDbList, Product::getId, Product::getCreateTime);
                    // 执行业务功能联动及状态补偿保障机制
                    if (RedisConstants.PRODUCT_CALC_QUEUE_KEY != null) {
                        redisService.setCacheSet(RedisConstants.PRODUCT_CALC_QUEUE_KEY, finalDbList.stream().map(p -> String.valueOf(p.getId())).collect(Collectors.toSet()));
                    }
                } catch (Exception e) {
                    log.error("Failed to rebuild product hot rank zset", e);
                }
            });

            // 本机内存基于 DB 列表截取分页
            int start = (pageNo - 1) * pageSize;
            int end = Math.min(start + pageSize, finalDbList.size());
            if (start >= finalDbList.size()) return Collections.emptyList();

            List<Product> pageList = finalDbList.subList(start, end);
            resultList = convertToProductVOList(pageList);
            return resultList;
        }

        // 6. 批量从数据库中获取商品实体列表
        List<Product> products = new ArrayList<>();
        if (CollUtil.isNotEmpty(productIdList)) {
            List<Product> unsortedProducts = query()
                    .in("id", productIdList)
                    .eq("status", ProductStatusEnum.ON_SHELF.getCode())
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .list();
            // 构建 Map 以重新保证结果集合按前序 ZSet 分数排列的有序一致性
            Map<Long, Product> productMap = unsortedProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
            for (Long id : productIdList) {
                Product p = productMap.get(id);
                if (p != null) {
                    products.add(p);
                }
            }
        }

        // 7. 将对应的商品实体列表批量转化为 VO
        resultList = convertToProductVOList(products);
        return resultList;
    }

    @Override
    public List<ProductVO> searchProducts(String keyword) {
        String trimmedKeyword = keyword == null ? null : keyword.trim();
        List<Product> products = query()
                .select("id", "name", "status", "audit_status")
                .like(StrUtil.isNotBlank(trimmedKeyword), "name", trimmedKeyword)
                .eq("status", ProductStatusEnum.ON_SHELF.getCode())
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .list();
        return convertToProductVOList(products);
    }

    /**
     * 发送商品动作消息到MQ
     *
     * @param productId 商品ID
     * @param itemActionType 动作类型
     */
    @Override
    public int addStock(Long id) {
        log.info("Sending product restock feed event");
        // 同步执行下游链路数据补偿保障
        sendProductActionMessageToMQ(id, ItemActionType.RESTOCK);
        return 1;
    }

    /**
     * 全量发布数据同步
     *
     * @return 结果
     */
    @Override
    public String allPublish() {
        int page = PageConstants.PAGE_NUMBER;
        int pageSize = PageConstants.ES_PAGE_SIZE;
        while (true) {
            // 同步执行下游链路数据补偿保障
            List<Product> products = query()
                    .eq("status", ProductStatusEnum.ON_SHELF.getCode())
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .page(new Page<>(page, pageSize))
                    .getRecords();
            if (products.isEmpty()) {
                break;
            }

            int finalPage = page;
            executorService.submit(()->{
                log.info("Publishing product page {} on thread {}", finalPage, Thread.currentThread().getName());
                // 同步执行下游链路数据补偿保障
                sendProductBatchMessage(products);
                log.info("Published product page {}, size {}", finalPage, products.size());
            });
            page++;
        }
        return "publish success";
    }


    /**
     * 发布指定商品数据同步
     *
     * @param ids 商品ID数组
     * @return 结果
     */
    @Override
    public String publish(String[] ids) {
        if (ids == null || ids.length == 0) {
            return "no ids to publish";
        }
        // Convert to Long list
        List<Long> idList = Arrays.stream(ids)
                .map(Long::valueOf)
                .collect(Collectors.toList());

        executorService.submit(() -> {
            log.info("Publishing products {} on thread {}", idList, Thread.currentThread().getName());
            // Batch query
            List<Product> products = query()
                    .in("id", idList)
                    .eq("status", ProductStatusEnum.ON_SHELF.getCode())
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .list();
            if (CollUtil.isNotEmpty(products)) {
                // Batch send message
                sendProductBatchMessage(products);
            }
        });
        return "publish success";
    }

    /**
     * 发送批量同步消息
     *
     * @param products 商品列表
     */
    private void sendProductBatchMessage(List<Product> products) {
        if (CollUtil.isEmpty(products)) {
            return;
        }
        ContentBatchSyncMessage request = new ContentBatchSyncMessage();
        request.setIndexName(EsIndexNameConstants.PRODUCT_INDEX_NAME);
        request.setData(products);
        request.setType(GlobalBizTypeEnum.PRODUCT.getCode());

        // 同步执行下游链路数据补偿保障
        mqMessageSendUtils.sendMqMessage( SearchMqConstants.ES_SYNC_EXCHANGE, SearchMqConstants.ES_SYNC_BATCH_INSERT_ROUTING_KEY, request);
        // 同步执行下游链路数据补偿保障
        mqMessageSendUtils.sendMqMessage( SearchMqConstants.MILVUS_SYNC_EXCHANGE, SearchMqConstants.MILVUS_SYNC_BATCH_INSERT_ROUTING_KEY, request);
    }

    /**
     * 清理单条商品缓存
     *
     * @param productId 商品ID
     */
    private void clearProductCache(Long productId) {
        if (productId == null) {
            return;
        }
        redisService.deleteObject(RedisConstants.CACHE_PRODUCT_KEY + productId);
    }

    /**
     * 批量清理商品缓存
     *
     * @param productIds 商品ID集合
     */
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
    }
    /**
     * 批量清理商品缓存
     *
     * @param productIds 商品ID集合
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
     * 批量清理商品缓存
     *
     * @param productIds 商品ID集合
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
     * 批量清理商品缓存
     *
     * @param productIds 商品ID集合
     */
    @Override
    public Boolean updateFansCountBatch(Map<Long, Integer> updateMap) {
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
                productMapper.updateFansCountBatch(batchMap);
            }
        } else {
            productMapper.updateFansCountBatch(updateMap);
        }
        clearProductCacheBatch(updateMap.keySet());
        return true;
    }

    /**
     * 批量更新商品销量
     *
     * @param updateMap 商品ID与增加的销量映射
     * @return 成功与否
     */
    @Override
    public Boolean updateSoldBatch(Map<Long, Integer> updateMap) {
        if (CollUtil.isEmpty(updateMap)) {
            return false;
        }
        // 同步执行下游链路数据补偿保障
        if (updateMap.size() > 500) {
            List<List<Long>> partition = ListUtil.partition(new ArrayList<>(updateMap.keySet()), 500);
            for (List<Long> batchKeys : partition) {
                Map<Long, Integer> batchMap = new HashMap<>();
                for (Long key : batchKeys) {
                    batchMap.put(key, updateMap.get(key));
                }
                productMapper.updateSoldBatch(batchMap);
            }
        } else {
            productMapper.updateSoldBatch(updateMap);
        }
        // 执行业务功能联动及状态补偿保障机制
        clearProductCacheBatch(updateMap.keySet());
        return true;
    }

    /**
     * 同步销量数据
     */
    @Override
    public void syncSalesData() {
        log.info("Starting product sales data sync");
        String countKeyPrefix = SalesTypeEnum.PRODUCT_SALES.getCountKeyPrefix();
        String dirtyKey = SalesTypeEnum.PRODUCT_SALES.getDirtyKey();
        String tempKey = dirtyKey + ":TEMP";

        // 构建 Map 以重新保证结果集合按前序 ZSet 分数排列的有序一致性
        redisService.syncDataWithSnapshot("product sales", countKeyPrefix, dirtyKey, tempKey,
                this::updateSoldBatch,
                updateMap -> enqueueIds(RedisConstants.PRODUCT_CALC_QUEUE_KEY, updateMap.keySet()));
        log.info("Product sales data sync finished");
    }



    /**
     * 加入重算队列
     *
     * @param key 队列Key
     * @param ids 商品ID集合
     */
    private void enqueueIds(String key, Collection<Long> ids) {
        if (key == null || CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            if (id != null) {
                redisService.setCacheSet(key, id.toString());
            }
        }
    }

    /**
     * 获取商品收藏数
     *
     * @param sourceId 商品ID
     * @return 收藏数
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
     * 发送新商品Feed流消息到MQ
     *
     * @param product 商品实体
     */
    @Override
    public Boolean updateProductStatus(Long id, Integer status, String reason) {
        Product product = getById(id);
        if (product == null) return false;

        Integer finalStatus = product.getStatus() == null ? ProductStatusEnum.OFF_SHELF.getCode() : product.getStatus();

        // 执行业务功能联动及状态补偿保障机制
        if (Objects.equals(status, AuditStatusEnum.PASS.getCode())) {
            finalStatus = ProductStatusEnum.ON_SHELF.getCode();
            // 执行业务功能联动及状态补偿保障机制
            if (product.getActivityType() != null && product.getActivityType() == 1) {
                Date now = DateUtils.getNowDate();
                // 同步执行下游链路数据补偿保障
                if (product.getBeginTime() != null && product.getBeginTime().getTime() > now.getTime()) {
                    finalStatus = ProductStatusEnum.OFF_SHELF.getCode(); // 执行业务功能联动及状态补偿保障机制
                }

                // 构建 Map 以重新保证结果集合按前序 ZSet 分数排列的有序一致性
                long preHeatTime = now.getTime() + (RedisConstants.SECKILL_PRE_HEAT_WINDOW_HOURS * 60 * 60 * 1000);
                if (product.getBeginTime() == null || product.getBeginTime().getTime() <= preHeatTime) {
                    redisService.setCacheObject(RedisConstants.SECKILL_STOCK_KEY + product.getId(), product.getStock());
                }
            }
            // 同步执行下游链路数据补偿保障
            sendNewProductMessageToMQ(product);
        } else if (AuditStatusEnum.isRejected(status)) {
            // 执行业务功能联动及状态补偿保障机制
            finalStatus = ProductStatusEnum.OFF_SHELF.getCode();
        }

        // 执行业务功能联动及状态补偿保障机制
        String rejectReason = AuditStatusEnum.isRejected(status) ? reason : null;
        boolean b = update(new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Product>()
                .set("status", finalStatus)
                .set("audit_status", status)
                .set("reject_reason", rejectReason)
                .eq("id", id));
        if(b){
            clearProductCache(id);
            // 执行业务功能联动及状态补偿保障机制
            if (Objects.equals(status, AuditStatusEnum.PASS.getCode())) {
                publish(new String[]{id.toString()});
                String rankKeySuffix = (product.getCategory() != null && product.getCategory() == 1) ? "voucher" : "deal";
                String hotRankKey = RedisConstants.PRODUCT_HOT_RANK_KEY + rankKeySuffix;
                double score = product.getCreateTime() != null ? (double) product.getCreateTime().getTime() : (double) System.currentTimeMillis();
                redisService.setCacheZSet(hotRankKey, id.toString(), score);
                redisService.setCacheSet(RedisConstants.PRODUCT_CALC_QUEUE_KEY, id.toString());
            } else {
                ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
                contentSyncMessage.setId(id);
                contentSyncMessage.setIndexName(EsIndexNameConstants.PRODUCT_INDEX_NAME);
                contentSyncMessage.setType(GlobalBizTypeEnum.PRODUCT.getCode());
                mqMessageSendUtils.sendMqMessage(SearchMqConstants.ES_SYNC_EXCHANGE, SearchMqConstants.ES_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
                mqMessageSendUtils.sendMqMessage(SearchMqConstants.MILVUS_SYNC_EXCHANGE, SearchMqConstants.MILVUS_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
            }
        }
        return b;
    }
    /**
     * 扣减数据库库存
     *
     * @param id 商品ID
     * @return 成功与否
     */
    @Override
    public boolean deductStock(Long id) {
        // Simple update: stock = stock - 1 where id = id and stock > 0
        return update().setSql("stock = stock - 1").eq("id", id).gt("stock", 0).update();
    }
    /**
     * 恢复商品库存
     *
     * @param id 商品ID
     * @param userId 用户ID
     * @return 成功与否
     */
    @Override
    public boolean recoverStock(Long id, Long userId) {
        recoverRedisStockAndEligibility(id, userId);
        // Simple update: stock = stock + 1 where id = id
        return update().setSql("stock = stock + 1").eq("id", id).update();
    }

    @Override
    public boolean recoverRedisStockAndEligibility(Long productId, Long userId) {
        if (productId == null) return false;
        
        // 1. 恢复 Redis 中的商品库存缓存
        String stockKey = "seckill:stock:" + productId;
        redisService.incrementCacheValue(stockKey, 1);
        
        // 2. 清除 Redis 中该用户购买记录，恢复用户的购买资格（如果传入了userId）
        if (userId != null) {
            String orderKey = "seckill:order:" + productId;
            Long l = stringRedisTemplate.opsForSet().remove(orderKey, userId.toString());
            log.info("成功清理 Redis 缓存订单购买记录标记, productId={}, userId={}, result={}", productId, userId, l);
        }
        
        log.info("成功执行恢复 Redis 商品库存及购买资格逻辑, productId={}, userId={}", productId, userId);
        return true;
    }

}
