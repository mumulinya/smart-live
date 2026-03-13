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
import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.*;
import com.smartLive.common.core.enums.*;
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
import java.util.function.Consumer;

import com.smartLive.product.domain.VO.ProductVO;
import com.smartLive.product.service.strategy.PurchaseStrategy;

import java.util.concurrent.TimeUnit;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.system.api.RemoteUserService;
import com.smartLive.shop.api.DTO.ShopDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.smartLive.product.mapper.ProductMapper;
import com.smartLive.product.domain.Product;
import com.smartLive.product.service.IProductService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品业务逻辑实现类
 * 
 * 核心职能：
 * 1. 采用策略模式（PurchaseStrategy）统一处理普通购买与秒杀抢购逻辑。
 * 2. 联动 Elasticsearch (ES) 与 Milvus (向量库) 实现双索引实时/批量同步。
 * 3. 基于 RabbitMQ 发起内容审核、Feed 流通知及库存补偿。
 * 4. 集成 Redis 实现多级缓存加载、热门商品排行榜 ZSet 维护及增量销量统计。
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
     * 查询商品
     *
     * @param id 商品主键
     * @return 商品VO
     */
    @Override
    public ProductVO selectProductById(Long id)
    {
        Product product = productMapper.selectProductById(id);
        return convertToProductVO(product);
    }

    /**
     * 查询商品实体（内部使用，含店铺信息）
     *
     * @param id 商品主键
     * @return 商品实体
     */
    @Override
    public Product selectProductEntityById(Long id)
    {
        Product product = productMapper.selectProductById(id);
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
     * 查询商品列表（VO）
     *
     * @param product 商品查询条件
     * @return 商品VO集合
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
            // 发送审核消息
            sendAuditMessage(product);
        }
        return i;
    }

    /**
     * 店铺发布新商品，发送MQ消息更新用户动态
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
     * 发送商品动态操作MQ消息（降价/重新上架/即将下架等）
     *
     * @param productId      商品ID
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

            // 修改商品后，防止其依然存在于秒杀缓存中导致被抢购，无条件删除Redis库存
            if(product.getActivityType() != null && product.getActivityType() == 1){
                redisService.deleteObject(RedisConstants.SECKILL_STOCK_KEY + product.getId());
                //更新秒杀库存
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
                    log.info("线程{}删除es数据id为：{}", Thread.currentThread().getName(), id);
                    ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
                    contentSyncMessage.setId(id);
                    contentSyncMessage.setIndexName(EsIndexNameConstants.PRODUCT_INDEX_NAME); // KEEP VOUCHER INDEX
                    contentSyncMessage.setType(GlobalBizTypeEnum.PRODUCT.getCode()); // KEEP VOUCHER
                    // 发起rabbitMq信息删除es数据
                    mqMessageSendUtils.sendMqMessage( SearchMqConstants.ES_SYNC_EXCHANGE, SearchMqConstants.ES_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
                    // 发起rabbitmq信息删除milvus数据
                    mqMessageSendUtils.sendMqMessage( SearchMqConstants.MILVUS_SYNC_EXCHANGE, SearchMqConstants.MILVUS_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
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
            redisService.deleteObject(RedisConstants.SECKILL_STOCK_KEY + id);
        }
        return rows;
    }


    /**
     * 购买商品（统一分发逻辑）
     * 
     * 流程：
     * 1. 校验商品存在性。
     * 2. 根据 activityType (0为普通, 1为秒杀) 从 purchaseStrategyMap 动态获取执行策略。
     * 3. 策略内部负责处理具体的库存扣减、限购检查及订单生成。
     *
     * @param productId 商品ID
     * @param userId    用户ID
     * @return 最终产生的订单 ID
     */
    @Override
    @Transactional
    public Long purchaseProduct(Long productId, Long userId) {
        Product product = getById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        // 根据活动类型选择策略：0-普通, 1-秒杀
        String strategyName = "NormalPurchaseStrategy";
        if (product.getActivityType() != null && product.getActivityType() == 1) {
            strategyName = "SeckillPurchaseStrategy";
        }

        PurchaseStrategy strategy = purchaseStrategyMap.get(strategyName);
        if (strategy == null) {
             throw new RuntimeException("系统配置错误：未找到对应的购买策略 " + strategyName);
        }

        return strategy.purchase(userId, product);
    }

    /**
     * 根据店铺查询商品列表
     *
     * @param product 商品查询条件（包含shopId和category）
     * @return 商品VO列表
     */
    @Override
    public List<ProductVO> queryProductOfShop(Product product) {
        List<Product> products = query()
                .apply("FIND_IN_SET({0}, shop_id)", product.getShopId())
                .eq("category", product.getCategory())
                .orderByAsc("create_time")
                .list();
        if (products.isEmpty()) {
            return null;
        }
        return convertToProductVOList(products);
    }


    /**
     * 发送审核消息到MQ
     *
     * @param product 商品实体
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
     * 查询全部商品列表（含店铺信息）
     *
     * @return 商品列表
     */
    @Override
    public List<Product> listProduct( ) {
        return query().list();
    }

    /**
     * 将Product实体转换为ProductVO
     *
     * @param product 商品实体
     * @return 商品VO对象
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
     * 将Product列表转换为ProductVO列表
     *
     * @param productList 商品实体列表
     * @return 商品VO列表
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
     * 获取商品列表
     *
     * @param sourceIdList 商品id列表
     * @return 商品列表
     */
    @Override
    public List<Product> getProductListByIds(List<Long> sourceIdList) {
        List<Product> productList = redisMultiCacheManager.queryBatchWithCache(
                RedisConstants.CACHE_PRODUCT_KEY,
                RedisConstants.LOCK_PRODUCT_KEY,
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
        return productList;
    }


    /**
     * 获取商品
     *
     * @param id 商品id
     * @return 商品VO
     */
    @Override
    public ProductVO getProductById(Long id) {
        Product product = cacheClient.queryWithLogicalExpireAndPassThrough(
                RedisConstants.CACHE_PRODUCT_KEY,
                RedisConstants.LOCK_PRODUCT_KEY,
                id,
                Product.class,
                productMapper::selectProductById,
                RedisConstants.CACHE_PRODUCT_TTL,
                TimeUnit.MINUTES
        );
        if (product == null){
            return null;
        }
        ProductVO productVO = convertToProductVO(product);
        // 判断是否收藏
        StarDTO starDTO=new StarDTO();
        starDTO.setSourceType(GlobalBizTypeEnum.PRODUCT.getCode()); // KEEP
        starDTO.setSourceId(id);
        Boolean isStar = remoteStarService.isStar(starDTO);
        productVO.setIsStar(isStar);
        // 判断是否关注
        FollowDTO followDTO=new FollowDTO();
        followDTO.setSourceType(GlobalBizTypeEnum.PRODUCT.getCode()); // KEEP
        followDTO.setSourceId(id);
        Boolean isFollow = remoteFollowService.isFollowed(followDTO);
        productVO.setIsFollow(isFollow);
        return productVO;
    }

    /**
     * 获取热门商品排行榜（支持分页与分类）
     * 
     * 实现细节：
     * 1. 优先从 Redis ZSet ( hot_rank:product:xxx ) 中获取已计算好的 ID 分页序列。
     * 2. 若 ZSet 击穿且为首页请求，则从数据库读取 (按销量+时间)，并异步触发重写 ZSet 任务。
     * 3. 返回的 VO 中附带 hotScore (热度分)，便于前端展示排名及热度值。
     *
     * @param current  当前页
     * @param size     页大小
     * @param category 种类 (1:代金券, 2:团购套餐)
     * @return 商品排行榜列表
     */
    @Override
    public List<ProductVO> getHotProductRank(Integer current, Integer size, Integer category) {
        int pageNo = current == null || current < 1 ? 1 : current;
        int pageSize = size == null || size < 1 ? 10 : size;

        // 1. 防御性拦截：最多只提供前 100 名的精选榜单
        if (pageNo * pageSize > 100) {
            return Collections.emptyList();
        }

        // 2. 根据 category 决定对应的 Redis Key
        String hotRankKey = RedisConstants.PRODUCT_HOT_RANK_KEY;
        if (ProductEnum.VOUCHER.getCode().equals(category)) {
            hotRankKey += "voucher";
        } else if (ProductEnum.SET_MEAL.getCode().equals(category)) {
            hotRankKey += "deal";
        } else {
            return Collections.emptyList();
        }

        List<ProductVO> resultList;

        // 3. 从最新的 ZSet 热度排行榜中获取商品 ID 分页数据
        Page<Long> longPage = zSetIdManager.pageIds(hotRankKey, null, current, SystemConstants.MAX_PAGE_SIZE);
        List<Long> productIdList = longPage.getRecords();

        if (CollUtil.isEmpty(productIdList)&&current == 1) {
            // ZSet 击穿或尚无数据时的兜底：查出全量数据写入 ZSet，再手动分页
            log.info("商品热榜 ZSet ({}) 为空，走数据库兜底查询", hotRankKey);
            List<Product> dbList = query()
                    .eq("category", category)
                    .orderByDesc("sold")
                    .orderByDesc("create_time")
                    .list();

            if (CollUtil.isEmpty(dbList)) return Collections.emptyList();

            // 仅取前 100
            List<Product> finalDbList = dbList.stream().limit(100).collect(Collectors.toList());
            final String finalHotRankKey = hotRankKey;
            // 开启异步线程写入 ZSet
            executorService.execute(() -> {
                try {
                    zSetIdManager.saveToZSet(finalHotRankKey, finalDbList, Product::getId, Product::getCreateTime);
                    // 顺便把 ID 塞入重算热度任务队列，等待凌晨重新计算正常的分数
                    if (RedisConstants.PRODUCT_CALC_QUEUE_KEY != null) {
                        redisService.setCacheSet(RedisConstants.PRODUCT_CALC_QUEUE_KEY, finalDbList.stream().map(p -> String.valueOf(p.getId())).collect(Collectors.toSet()));
                    }
                } catch (Exception e) {
                    log.error("兜底重写商品热榜到 ZSet 失败", e);
                }
            });

            // 手动对 dbList 进行分页
            int start = (pageNo - 1) * pageSize;
            int end = Math.min(start + pageSize, finalDbList.size());
            if (start >= finalDbList.size()) return Collections.emptyList();

            List<Product> pageList = finalDbList.subList(start, end);
            resultList = convertToProductVOList(pageList);
            return resultList;
        }

        // 6. 分批查询数据库，并按 ZSet 的热度分数顺序进行排序重组
        List<Product> products = new ArrayList<>();
        if (CollUtil.isNotEmpty(productIdList)) {
            List<Product> unsortedProducts = query().in("id", productIdList).list();
            // 按照 pageIds 的顺序重组
            Map<Long, Product> productMap = unsortedProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
            for (Long id : productIdList) {
                Product p = productMap.get(id);
                if (p != null) {
                    products.add(p);
                }
            }
        }

        // 7. 转换为 VO
        resultList = convertToProductVOList(products);
        return resultList;
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
                // 发送批量消息
                sendProductBatchMessage(products);
                log.info("发送第 {} 页，{} 条数据", finalPage, products.size());
            });
            page++;
        }
        return "数据发布完成";
    }


    /**
     * 批量发布商品至ES和Milvus索引
     *
     * @param ids 商品ID数组
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
                // Batch send message
                sendProductBatchMessage(products);
            }
        });
        return "发布成功";
    }

    /**
     * 批量发送ES和Milvus同步消息
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

        // 发送rabbitmq消息数据插入es
        mqMessageSendUtils.sendMqMessage( SearchMqConstants.ES_SYNC_EXCHANGE, SearchMqConstants.ES_SYNC_BATCH_INSERT_ROUTING_KEY, request);
        // 发送rabbitmq消息数据插入Milvus
        mqMessageSendUtils.sendMqMessage( SearchMqConstants.MILVUS_SYNC_EXCHANGE, SearchMqConstants.MILVUS_SYNC_BATCH_INSERT_ROUTING_KEY, request);
    }

    /**
     * 清空单个商品缓存（详情 + 秒杀库存）
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
     * 批量清空商品缓存（详情 + 秒杀库存）
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
     * 批量更新商品评价数
     *
     * @param updateMap 商品ID与评价数的映射
     * @return 更新结果
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
     * 批量更新商品收藏数
     *
     * @param updateMap 商品ID与收藏数的映射
     * @return 更新结果
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
     * 批量更新商品粉丝数
     *
     * @param updateMap 商品ID与粉丝数的映射
     * @return 更新结果
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
     * 批量更新销量
     *
     * @param updateMap 商品ID与销量的映射
     * @return 更新结果
     */
    /**
     * 批量更新销量
     *
     * @param updateMap 商品ID与销量的映射
     * @return 更新结果
     */
    @Override
    public Boolean updateSoldBatch(Map<Long, Integer> updateMap) {
        if (CollUtil.isEmpty(updateMap)) {
            return false;
        }
        // 分批处理，防止 SQL 语句过长
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
        // 清除受影响商品的缓存
        clearProductCacheBatch(updateMap.keySet());
        return true;
    }

    /**
     * 定时任务执逻辑：同步商品销量数据从 Redis 到数据库
     */
    @Override
    public void syncSalesData() {
        log.info("开始定时任务：同步商品销量数据");
        String countKeyPrefix = SalesTypeEnum.PRODUCT_SALES.getCountKeyPrefix();
        String dirtyKey = SalesTypeEnum.PRODUCT_SALES.getDirtyKey();
        String tempKey = dirtyKey + ":TEMP";

        // 执行同步逻辑，完成后更新热度计算队列
        redisService.syncDataWithSnapshot("商品销量", countKeyPrefix, dirtyKey, tempKey,
                this::updateSoldBatch,
                updateMap -> enqueueIds(RedisConstants.PRODUCT_CALC_QUEUE_KEY, updateMap.keySet()));
        log.info("定时任务：同步商品销量数据完成");
    }



    /**
     * 批量将 ID 存入 Redis Set 集合中（供热度计算队列使用）
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
     * @return 收藏数量
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
     * 更新商品状态（审核通过/拒绝）
     *
     * @param id     商品ID
     * @param status 商品状态
     * @return 更新结果
     */
    @Override
    public Boolean updateProductStatus(Long id, Integer status, String reason) {
        Product product = getById(id);
        if (product == null) return false;

        Integer finalStatus = status;

        // 如果审核通过
        if (status.equals(ProductStatusEnum.NORMAL.getCode())) {
            // 如果是秒杀商品
            if (product.getActivityType() != null && product.getActivityType() == 1) {
                Date now = DateUtils.getNowDate();
                // 判断是否未到开始时间
                if (product.getBeginTime() != null && product.getBeginTime().getTime() > now.getTime()) {
                    finalStatus = ProductStatusEnum.OFF_SHELF.getCode(); // 未上架
                }

                // 按预热窗口初始化Redis库存
                long preHeatTime = now.getTime() + (RedisConstants.SECKILL_PRE_HEAT_WINDOW_HOURS * 60 * 60 * 1000);
                if (product.getBeginTime() == null || product.getBeginTime().getTime() <= preHeatTime) {
                    redisService.setCacheObject(RedisConstants.SECKILL_STOCK_KEY + product.getId(), product.getStock());
                }
            }
            // 发送消息推送动态
            sendNewProductMessageToMQ(product);
        }

        // 拒绝时写入拒绝原因，通过时清空拒绝原因
        String rejectReason = AuditStatusEnum.isRejected(status) ? reason : null;
        boolean b = update(new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Product>()
                .set("status", finalStatus)
                .set("reject_reason", rejectReason)
                .eq("id", id));
        if(b){
            clearProductCache(id);
            // 如果审核通过，则将其加入Redis热榜和计算队列
            if (status.equals(ProductStatusEnum.NORMAL.getCode())) {
                publish(new String[]{id.toString()});
                String rankKeySuffix = (product.getCategory() != null && product.getCategory() == 1) ? "voucher" : "deal";
                String hotRankKey = RedisConstants.PRODUCT_HOT_RANK_KEY + rankKeySuffix;
                double score = product.getCreateTime() != null ? (double) product.getCreateTime().getTime() : (double) System.currentTimeMillis();
                redisService.setCacheZSet(hotRankKey, id.toString(), score);
                redisService.setCacheSet(RedisConstants.PRODUCT_CALC_QUEUE_KEY, id.toString());
            }
        }
        return b;
    }
    /**
     * 扣减库存（库存-1，仅库存>0时成功）
     *
     * @param id 商品ID
     * @return 扣减结果
     */
    @Override
    public boolean deductStock(Long id) {
        // Simple update: stock = stock - 1 where id = id and stock > 0
        return update().setSql("stock = stock - 1").eq("id", id).gt("stock", 0).update();
    }
    /**
     * 恢复库存（库存+1，用于订单取消/超时回滚）
     *
     * @param id 商品ID
     * @return 恢复结果
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
        
        // 1. 恢复 Redis 预扣库存
        String stockKey = "seckill:stock:" + productId;
        redisService.incrementCacheValue(stockKey, 1);
        
        // 2. 移除用户重复下单限制记录 (如果提供了 userId)
        if (userId != null) {
            String orderKey = "seckill:order:" + productId;
            Long l = stringRedisTemplate.opsForSet().remove(orderKey, userId.toString());
            log.info("已移除用户下单限制: productId={}, userId={}, result={}", productId, userId, l);
        }
        
        log.info("已回滚 Redis 秒杀数据: productId={}, userId={}", productId, userId);
        return true;
    }

}
