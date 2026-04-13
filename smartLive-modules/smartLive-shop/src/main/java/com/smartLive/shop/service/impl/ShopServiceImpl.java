package com.smartLive.shop.service.impl;
import com.smartLive.common.core.constant.mq.SearchMqConstants;
import com.smartLive.common.core.constant.mq.AiAuditMqConstants;

import com.smartLive.common.core.enums.interaction.FollowTypeEnum;
import com.smartLive.common.core.enums.interaction.ReviewTypeEnum;
import com.smartLive.common.core.enums.interaction.StarTypeEnum;
import com.smartLive.common.core.enums.product.SalesTypeEnum;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.*;
import com.smartLive.common.core.context.SecurityContextHolder;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.AppLoginUser;
import com.smartLive.common.security.utils.SecurityUtils;
import com.smartLive.common.rabbitmq.domain.AuditMessage;
import com.smartLive.common.rabbitmq.domain.ContentBatchSyncMessage;
import com.smartLive.common.rabbitmq.domain.ContentSyncMessage;
import com.smartLive.common.rabbitmq.domain.MqSendMode;
import com.smartLive.common.core.enums.common.AuditStatusEnum;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.utils.StringUtils;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.common.redis.util.RedisMultiCacheManager;
import com.smartLive.interaction.api.RemoteFollowService;
import com.smartLive.interaction.api.RemoteStarService;
import com.smartLive.interaction.api.DTO.BadReviewDTO;
import com.smartLive.interaction.api.DTO.ShopReviewSuggestDTO;
import com.smartLive.interaction.api.RemoteReviewService;
import com.smartLive.order.api.DTO.ProductSalesDTO;
import com.smartLive.order.api.DTO.ShopOrderSuggestDTO;
import com.smartLive.order.api.RemoteOrderService;
import com.smartLive.shop.domain.VO.*;
import com.smartLive.system.api.RemoteUserService;
import com.smartLive.interaction.api.DTO.FollowDTO;
import com.smartLive.interaction.api.DTO.StarDTO;
import com.smartLive.shop.domain.ShopType;
import com.smartLive.shop.service.IShopTypeService;
import com.smartLive.common.redis.util.CacheClient;
import com.smartLive.common.redis.util.ZSetIdManager;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.lucene.util.SloppyMath;
import org.springframework.beans.BeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartLive.shop.mapper.ShopMapper;
import com.smartLive.shop.domain.Shop;
import com.smartLive.shop.service.IShopService;
import jakarta.annotation.Resource;

/**
 * 店铺服务实现类。
 */
@Service
@Slf4j
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Autowired
    private ShopMapper shopMapper;
    @Autowired
    private IShopTypeService shopTypeService;

    @Autowired
    private MqMessageSendUtils mqMessageSendUtils;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private RemoteStarService remoteStarService;
    @Autowired
    RemoteFollowService remoteFollowService;
    @Autowired
    private RemoteUserService remoteUserService;
    @Autowired
    private RemoteOrderService remoteOrderService;
    @Autowired
    private RemoteReviewService remoteReviewService;
    @Autowired
    private RedisMultiCacheManager redisMultiCacheManager;
    @Autowired
    private ZSetIdManager zSetIdManager;
    /**
     * 将店铺实体转换为店铺视图对象。
     */
    private ShopVO toShopVO(Shop shop) {
        if (shop == null) {
            return null;
        }
        ShopVO shopVO = new ShopVO();
        BeanUtils.copyProperties(shop, shopVO);
        return shopVO;
    }

    private ShopVO convertToShopVO(Shop shop) {
        ShopVO shopVO = toShopVO(shop);
        fillShopDynamicStats(Collections.singletonList(shopVO));
        return shopVO;
    }

    /**
     * 判断店铺是否处于可展示状态。
     */
    private boolean isVisibleShop(Shop shop) {
        return shop != null
                && Objects.equals(shop.getStatus(), 1)
                && Objects.equals(shop.getAuditStatus(), AuditStatusEnum.PASS.getCode());
    }

    /**
     * 判断店铺是否处于可展示状态。
     */
    private boolean isVisibleShop(ShopVO shopVO) {
        return shopVO != null
                && Objects.equals(shopVO.getStatus(), 1)
                && Objects.equals(shopVO.getAuditStatus(), AuditStatusEnum.PASS.getCode());
    }

    /**
     * 批量将店铺实体转换为店铺视图对象。
     */
    private List<ShopVO> convertToShopVOList(List<Shop> shopList) {
        if (CollUtil.isEmpty(shopList)) {
            return new ArrayList<>();
        }
        List<ShopVO> shopVOList = shopList.stream()
                .map(this::toShopVO)
                .collect(Collectors.toList());
        fillShopDynamicStats(shopVOList);
        return shopVOList;
    }

    private void fillShopDynamicStats(List<ShopVO> shopVOList) {
        if (CollUtil.isEmpty(shopVOList)) {
            return;
        }
        List<ShopVO> validShopVOList = shopVOList.stream()
                .filter(shopVO -> shopVO != null && shopVO.getId() != null)
                .toList();
        if (CollUtil.isEmpty(validShopVOList)) {
            return;
        }
        List<Long> shopIds = validShopVOList.stream()
                .map(ShopVO::getId)
                .distinct()
                .collect(Collectors.toList());

        String starKeyPrefix = StarTypeEnum.SHOP_STAR.getStarCountKeyPrefix();
        String fansKeyPrefix = FollowTypeEnum.SHOP_IDENTITY.getFansCountKeyPrefix();
        String reviewKeyPrefix = ReviewTypeEnum.SHOP_REVIEW.getReviewCountKeyPrefix();
        String soldKeyPrefix = SalesTypeEnum.SHOP_SALES.getCountKeyPrefix();

        List<Integer> staredValues = redisService.getMultiCacheObject(shopIds.stream().map(id -> starKeyPrefix + id).collect(Collectors.toList()));
        List<Integer> fansValues = redisService.getMultiCacheObject(shopIds.stream().map(id -> fansKeyPrefix + id).collect(Collectors.toList()));
        List<Integer> reviewValues = redisService.getMultiCacheObject(shopIds.stream().map(id -> reviewKeyPrefix + id).collect(Collectors.toList()));
        List<Integer> soldValues = redisService.getMultiCacheObject(shopIds.stream().map(id -> soldKeyPrefix + id).collect(Collectors.toList()));

        Map<Long, Integer> staredMap = new HashMap<>(shopIds.size());
        Map<Long, Integer> fansMap = new HashMap<>(shopIds.size());
        Map<Long, Integer> reviewMap = new HashMap<>(shopIds.size());
        Map<Long, Integer> soldMap = new HashMap<>(shopIds.size());
        Set<Long> missingIds = new HashSet<>();
        fillCounterMapFromRedis(shopIds, staredValues, staredMap, missingIds);
        fillCounterMapFromRedis(shopIds, fansValues, fansMap, missingIds);
        fillCounterMapFromRedis(shopIds, reviewValues, reviewMap, missingIds);
        fillCounterMapFromRedis(shopIds, soldValues, soldMap, missingIds);

        if (CollUtil.isNotEmpty(missingIds)) {
            Map<Long, Shop> fallbackShopMap = query()
                    .select("id", "stared", "fans", "reviews", "sold")
                    .in("id", missingIds)
                    .list()
                    .stream()
                    .filter(shop -> shop != null && shop.getId() != null)
                    .collect(Collectors.toMap(Shop::getId, java.util.function.Function.identity(), (left, right) -> left));
            Map<String, Integer> cacheMap = new HashMap<>(missingIds.size() * 4);
            for (Long shopId : missingIds) {
                Shop fallbackShop = fallbackShopMap.get(shopId);
                if (!staredMap.containsKey(shopId)) {
                    Integer stared = fallbackShop != null && fallbackShop.getStared() != null ? fallbackShop.getStared() : 0;
                    staredMap.put(shopId, stared);
                    cacheMap.put(starKeyPrefix + shopId, stared);
                }
                if (!fansMap.containsKey(shopId)) {
                    Integer fans = fallbackShop != null && fallbackShop.getFans() != null ? fallbackShop.getFans() : 0;
                    fansMap.put(shopId, fans);
                    cacheMap.put(fansKeyPrefix + shopId, fans);
                }
                if (!reviewMap.containsKey(shopId)) {
                    Integer reviews = fallbackShop != null && fallbackShop.getReviews() != null ? fallbackShop.getReviews() : 0;
                    reviewMap.put(shopId, reviews);
                    cacheMap.put(reviewKeyPrefix + shopId, reviews);
                }
                if (!soldMap.containsKey(shopId)) {
                    Integer sold = fallbackShop != null && fallbackShop.getSold() != null ? fallbackShop.getSold() : 0;
                    soldMap.put(shopId, sold);
                    cacheMap.put(soldKeyPrefix + shopId, sold);
                }
            }
            if (CollUtil.isNotEmpty(cacheMap)) {
                redisService.setMultiCacheObject(cacheMap);
            }
        }

        validShopVOList.forEach(shopVO -> {
            Long shopId = shopVO.getId();
            shopVO.setStared(staredMap.getOrDefault(shopId, 0));
            shopVO.setFans(fansMap.getOrDefault(shopId, 0));
            shopVO.setReviews(reviewMap.getOrDefault(shopId, 0));
            shopVO.setSold(soldMap.getOrDefault(shopId, 0));
        });
    }

    private void fillCounterMapFromRedis(List<Long> ids, List<Integer> values, Map<Long, Integer> counterMap, Set<Long> missingIds) {
        for (int i = 0; i < ids.size(); i++) {
            Integer value = values != null && values.size() > i ? values.get(i) : null;
            if (value != null) {
                counterMap.put(ids.get(i), value);
            } else {
                missingIds.add(ids.get(i));
            }
        }
    }
    /**
     * 根据ID查询店铺实体。
     */
    @Override
    public Shop selectShopById(Long id) {
        return shopMapper.selectShopById(id);
    }

    /**
     * 查询店铺实体列表。
     */
    @Override
    public List<Shop> selectShopList(Shop shop) {
        if (shop == null) {
            shop = new Shop();
        }
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId != null && !SecurityUtils.isAdmin(currentUserId)) {
            return selectShopListByUserId(currentUserId, shop);
        }
        return shopMapper.selectShopList(shop);
    }

    /**
     * 新增店铺。
     */
    @Override
    @GlobalTransactional(name = "shop-insert-with-user-relation", rollbackFor = Exception.class)
    @Transactional(rollbackFor = Exception.class)
    public int insertShop(Shop shop) {
        shop.setCreateTime(DateUtils.getNowDate());
        int i = shopMapper.insertShop(shop);
        if (i > 0) {
            Long userId = SecurityUtils.getUserId();
            if (userId == null) {
                throw new BusinessException("user id is required");
            }
            if (shop.getId() == null) {
                throw new BusinessException("shop id was not generated");
            }
            Boolean relationSaved = remoteUserService.addUserShopRelation(userId, shop.getId());
            if (!Boolean.TRUE.equals(relationSaved)) {
                throw new BusinessException("failed to save user-shop relation");
            }
            sendAuditMessage(shop, MqSendMode.SYNC_RETRY_THROW);
            flashShopListRedisCache(shop.getTypeId());
        }
        return i;
    }

    /**
     * 更新店铺。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateShop(Shop shop) {
        Shop oldShop = shop.getId() == null ? null : shopMapper.selectById(shop.getId());
        shop.setUpdateTime(DateUtils.getNowDate());
        shop.setAuditStatus(AuditStatusEnum.WAITING.getCode());
        shop.setRejectReason(null);
        int i = shopMapper.updateShop(shop);
        if(i > 0){
            sendAuditMessage(shop, MqSendMode.SYNC_RETRY_THROW);
            Long shopId = shop.getId();
            flashShopRedisCache(shopId);
            if (oldShop != null && oldShop.getTypeId() != null) {
                flashShopListRedisCache(oldShop.getTypeId());
            }
            if (shop.getTypeId() != null && (oldShop == null || !Objects.equals(oldShop.getTypeId(), shop.getTypeId()))) {
                flashShopListRedisCache(shop.getTypeId());
            }
            publish(new String[]{shopId.toString()});
        }
        return i;
    }

    /**
     * 批量删除店铺。
     */
    @Override
    @GlobalTransactional(name = "shop-delete-batch-with-user-relation", rollbackFor = Exception.class)
    @Transactional(rollbackFor = Exception.class)
    public int deleteShopByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        List<Shop> shops = Arrays.stream(ids)
                .map(shopMapper::selectShopById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<Long> shopIdList = Arrays.stream(ids)
                .map(id -> {
                    try {
                        return Long.valueOf(id);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int i = shopMapper.deleteShopByIds(ids);
        if (i > 0) {
            if (CollUtil.isNotEmpty(shopIdList)) {
                Boolean relationDeleted = remoteUserService.deleteUserShopRelationByShopIds(shopIdList.toArray(new Long[0]));
                if (!Boolean.TRUE.equals(relationDeleted)) {
                    throw new BusinessException("failed to delete user-shop relations");
                }
            }
            for (Long id : ids) {
                executorService.submit(() -> {
                    log.info("async delete shop thread={}, shopId={}", Thread.currentThread().getName(), id);
                    Long shopId = id;
                    ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
                    contentSyncMessage.setId(shopId);
                    contentSyncMessage.setIndexName(EsIndexNameConstants.SHOP_INDEX_NAME);
                    contentSyncMessage.setType(GlobalBizTypeEnum.SHOP.getCode());
                    mqMessageSendUtils.sendMqMessage(SearchMqConstants.ES_SYNC_EXCHANGE, SearchMqConstants.ES_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
                    mqMessageSendUtils.sendMqMessage(SearchMqConstants.MILVUS_SYNC_EXCHANGE, SearchMqConstants.MILVUS_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
                });
            }
            Arrays.stream(ids)
                    .filter(Objects::nonNull)
                    .forEach(this::flashShopRedisCache);
            shops.stream()
                    .map(Shop::getTypeId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet())
                    .forEach(this::flashShopListRedisCache);
        }
        return i;
    }

    /**
     * 根据ID删除店铺。
     */
    @Override
    @GlobalTransactional(name = "shop-delete-with-user-relation", rollbackFor = Exception.class)
    @Transactional(rollbackFor = Exception.class)
    public int deleteShopById(Long id) {
        Shop shop = shopMapper.selectShopById(id);
        int i = shopMapper.deleteShopById(id);
        if (i > 0) {
            Long shopId = shop != null ? shop.getId() : id;
            if (shopId != null) {
                flashShopRedisCache(shopId);
            }

            if (shopId != null) {
                Boolean relationDeleted = remoteUserService.deleteUserShopRelationByShopId(shopId);
                if (!Boolean.TRUE.equals(relationDeleted)) {
                    throw new BusinessException("failed to delete user-shop relation");
                }
            }
            if (shop != null && shop.getTypeId() != null) {
                flashShopListRedisCache(shop.getTypeId());
            }
        }
        return i;
    }

    @Resource
    private CacheClient cacheClient;

    /**
     * 查询店铺详情。
     */
    @Override
    public ShopVO queryById(Long id) {
        Shop shop = cacheClient.queryWithLogicalExpireAndPassThrough(
                RedisConstants.CACHE_SHOP_KEY,
                RedisConstants.LOCK_SHOP_KEY,
                id,
                Shop.class,
                shopId -> query()
                        .eq("id", shopId)
                        .eq("status", 1)
                        .eq("audit_status", AuditStatusEnum.PASS.getCode())
                        .one(),
                RedisConstants.CACHE_SHOP_TTL,
                TimeUnit.MINUTES
        );
        if (shop == null) {
            return null;
        }
        if (!isVisibleShop(shop)) {
            return null;
        }
        ShopVO shopVO = convertToShopVO(shop);
        isShopStared(shopVO);
        isShopFollowed(shopVO);
        return shopVO;
    }

    /**
     * 判断当前用户是否已收藏店铺。
     */
    private void isShopStared(ShopVO shopVO) {
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            shopVO.setIsStared(false);
            return;
        }
        Long userId = user.getId();
        StarDTO starDTO = new StarDTO();
        starDTO.setUserId(userId);
        starDTO.setSourceId(shopVO.getId());
        starDTO.setSourceType(GlobalBizTypeEnum.SHOP.getCode());
        Boolean isStared = remoteStarService.isStar(starDTO);
        shopVO.setIsStared(isStared);
    }
    /**
     * 判断当前用户是否已关注店铺。
     */
    private void isShopFollowed(ShopVO shopVO) {
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            shopVO.setIsFollowed(false);
            return;
        }
        Long userId = user.getId();
        FollowDTO followDTO = new FollowDTO();
        followDTO.setUserId(userId);
        followDTO.setSourceId(shopVO.getId());
        followDTO.setSourceType(GlobalBizTypeEnum.SHOP.getCode());
        Boolean isFollowed = remoteFollowService.isFollowed(followDTO);
        shopVO.setIsFollowed(isFollowed);
    }
    /**
     * 使用缓存穿透方案查询店铺。
     */
    public ShopVO queryWithPassThrough(Long id) {

        String key = RedisConstants.CACHE_SHOP_KEY + id;
        String shopJson = redisService.getCacheObject(key);
        if (StrUtil.isNotBlank(shopJson)) {
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return convertToShopVO(shop);
        }
        if (shopJson != null) {
            return null;
        }
        Shop shop = this.getById(id);
        if (shop == null) {
            redisService.setCacheObject(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        redisService.setCacheObject(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return convertToShopVO(shop); // 转换为店铺视图对象后返回

    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 使用逻辑过期方案查询店铺。
     */
    public Shop queryWithLogicalExpire(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        String shopJson = redisService.getCacheObject(key);
        if (StrUtil.isBlank(shopJson)) {
            return null;
        }
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        if (redisData.getExpireTime().isAfter(LocalDateTime.now())) {
            return shop;
        }
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        if (isLock) {
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    this.saveHotShopRedis(id, 20L);
                } catch (Exception e) {

                } finally {
                    unLock(lockKey);
                }
            });
        }
        return shop;

    }


    /**
     * 使用互斥锁方案查询店铺。
     */
    public Shop queryWithMutex(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        String shopJson = redisService.getCacheObject(key);
        if (StrUtil.isNotBlank(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        if (shopJson != null) {
            return null;
        }

        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            if (!isLock) {
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            shop = this.getById(id);
            if (shop == null) {
                redisService.setCacheObject(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            redisService.setCacheObject(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
        } finally {
            unLock(lockKey);
        }
        unLock(lockKey);
        return shop;

    }

    /**
     * 尝试获取互斥锁。
     */
    private boolean tryLock(String key) {
        boolean flag = redisService.setCacheObjectIfAbsent(key, "1", RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放互斥锁。
     */
    private void unLock(String key) {
        redisService.deleteObject(key);
    }

    /**
     * 保存热门店铺缓存。
     */
    public void saveHotShopRedis(Long id, Long expireSeconds) throws InterruptedException {
        Shop shop = this.getById(id);
        Thread.sleep(2000);
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        redisService.setCacheObject(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 根据店铺名称查询店铺。
     */
    @Override
    public ShopVO getShopByShopName(String shopName) {
        Shop shop = query()
                .eq("name", shopName)
                .eq("status", 1)
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .one();
        return convertToShopVO(shop);
    }
    /**
     * 根据条件查询店铺。
     */
    @Override
    public List<ShopVO> getShopByCondition(Shop shop) {
        QueryWrapper<Shop> wrapper = new QueryWrapper<>();
        String distanceSql = "ST_Distance_Sphere(point(x, y), point(" + shop.getX() + ", " + shop.getY() + ")) as distance";
        wrapper.select("*, " + distanceSql);
        wrapper.eq("status", 1);
        wrapper.eq("audit_status", AuditStatusEnum.PASS.getCode());
        if (shop.getTypeId() != null) {
            wrapper.eq("type_id", shop.getTypeId());
        }

        if (StringUtils.isNotBlank(shop.getName()) ||
                StringUtils.isNotBlank(shop.getArea()) ||
                StringUtils.isNotBlank(shop.getAddress())) {

            wrapper.and(w -> w
                    .like(StringUtils.isNotBlank(shop.getName()), "name", shop.getName())
                    .or()
                    .like(StringUtils.isNotBlank(shop.getArea()), "area", shop.getArea())
                    .or()
                    .like(StringUtils.isNotBlank(shop.getAddress()), "address", shop.getAddress())
            );
        }

        if (shop.getX() != null && shop.getY() != null ) {
            wrapper.apply("ST_Distance_Sphere(point(x, y), point({0}, {1})) <= {2}",
                    shop.getX(), shop.getY(), 200000);
        }
        wrapper .orderByAsc("distance");
        return convertToShopVOList(list(wrapper));
    }

    /**
     * 批量查询店铺列表。
     */
    @Override
    public List<ShopVO> getShopList(List<Long> ids) {
        List<ShopVO> shopList = redisMultiCacheManager.queryBatchWithCache(
                RedisConstants.CACHE_SHOP_KEY,
                RedisConstants.LOCK_SHOP_KEY,
                ids,
                ShopVO.class,
                missingIds -> {
                    List<Shop> shops = query()
                            .in("id", missingIds)
                            .eq("status", 1)
                            .eq("audit_status", AuditStatusEnum.PASS.getCode())
                            .list();
                    return convertToShopVOList(shops);
                },
                ShopVO::getId,
                RedisConstants.CACHE_SHOP_TTL,
                TimeUnit.MINUTES
        );
        if (CollUtil.isEmpty(shopList)) {
            return Collections.emptyList();
        }
        fillShopDynamicStats(shopList);
        return shopList.stream()
                .filter(this::isVisibleShop)
                .collect(Collectors.toList());
    }

    /**
     * 获取店铺总数。
     */
    @Override
    public Integer getShopTotal() {
        return query().count().intValue();
    }

    /**
     * 查询最近新增的店铺。
     */
    @Override
    public List<ShopVO> getRecentShops(Integer limit) {
        return convertToShopVOList(query()
                .eq("status", 1)
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .orderByDesc("create_time")
                .last("limit " + limit)
                .list());
    }

    /**
     * 刷新店铺缓存。
     */
    @Override
    public String flushCache() {
        String key = RedisConstants.CACHE_SHOP_lIST_KEY+"*";
        redisService.deleteObject(redisService.keys(key));
        List<ShopType> shopTypeList = shopTypeService.list();
        shopTypeList.forEach(shopType -> {
            List<Shop> shopList = query()
                    .eq("type_id", shopType.getId())
                    .eq("status", 1)
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .list();
            redisService.setCacheObject(RedisConstants.CACHE_SHOP_lIST_KEY+shopType.getId(), shopList);
        });


        List<Shop> list = query()
                .eq("status", 1)
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .list();
        redisService.deleteObject(redisService.keys(RedisConstants.SHOP_GEO_KEY+"*"));
        Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            Long typeId = entry.getKey();
            String shopGeoKey = RedisConstants.SHOP_GEO_KEY + typeId;
            List<Shop> shopList = entry.getValue();
            for (Shop shop : shopList) {
                redisService.addCacheGeoLocation(shopGeoKey, shop.getX(), shop.getY(), shop.getId().toString());
            }
        }
        return "shop geo cache rebuilt";
    }
    /**
     * 发送店铺审核消息。
     */
    private void sendAuditMessage(Shop shop) {
        sendAuditMessage(shop, MqSendMode.ASYNC_RETRY);
    }

    private void sendAuditMessage(Shop shop, MqSendMode sendMode) {
        AuditMessage auditMessage = AuditMessage.builder()
                .bizId(shop.getId())
                .bizType(GlobalBizTypeEnum.SHOP.getCode())
                .submitterId(SecurityContextHolder.getUserId())
                .auditContent(BeanUtil.beanToMap(shop))
                .createTime(shop.getCreateTime())
                .build();
        mqMessageSendUtils.sendMqMessage(
                AiAuditMqConstants.AUDIT_DIRECT_EXCHANGE,
                AiAuditMqConstants.AUDIT_ROUTING_KEY,
                auditMessage,
                sendMode
        );
    }
    /**
     * 刷新店铺列表缓存。
     */
    private void flashShopListRedisCache(Long typeId) {
        redisService.deleteObject(RedisConstants.CACHE_SHOP_lIST_KEY+typeId);
    }

    /**
     * 刷新店铺详情缓存。
     */
    private void flashShopRedisCache(Long id){
        redisService.deleteObject(RedisConstants.CACHE_SHOP_KEY+id);
    }

    /**
     * 发布店铺数据到搜索服务。
     */
    @Override
    public String allPublish() {
        int page = PageConstants.PAGE_NUMBER;
        int pageSize = PageConstants.ES_PAGE_SIZE;
        while (true) {
            List<Shop> shops = query()
                    .eq("status", 1)
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .page(new Page<>(page, pageSize))
                    .getRecords();
            if (shops.isEmpty()) {
                break;
            }
            int finalPage = page;
            executorService.submit(()->{
                List<ShopVO> voList = convertToShopVOList(shops);
                voList.forEach(vo -> vo.setLocation(vo.getY() + "," + vo.getX()));
                sendShopBatchMessage(voList);
                log.info("async sync shop page thread={}, page={}, size={}", Thread.currentThread().getName(), finalPage, shops.size());
            });
            page++;
        }
        return "shop sales sync task submitted";
    }


    /**
     * 按ID发布店铺数据到搜索服务。
     */
    @Override
    public String publish(String[] ids) {
        if (ids == null || ids.length == 0) {
            return "ids are required";
        }
        // 转换为 Long 类型店铺ID列表。
        List<Long> idList = Arrays.stream(ids)
                .map(Long::valueOf)
                .collect(Collectors.toList());

        executorService.submit(() -> {
            log.info("async publish shop batch thread={}, ids={}", Thread.currentThread().getName(), idList);
            // 批量查询可发布的店铺数据。
            List<Shop> shops = query()
                    .in("id", idList)
                    .eq("status", 1)
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .list();
            if (CollUtil.isNotEmpty(shops)) {
                List<ShopVO> voList = convertToShopVOList(shops);
                // 补充位置坐标字符串。
                voList.forEach(vo -> vo.setLocation(vo.getY() + "," + vo.getX()));
                // 批量发送搜索同步消息。
                sendShopBatchMessage(voList);
            }
        });
        return "publish task submitted";
    }

    /**
     * 发送店铺批量同步消息。
     */
    private void sendShopBatchMessage(List<?> shops) {
        if (CollUtil.isEmpty(shops)) {
            return;
        }
        ContentBatchSyncMessage request = new ContentBatchSyncMessage();
        request.setIndexName(EsIndexNameConstants.SHOP_INDEX_NAME);
        request.setData(shops);
        request.setType(GlobalBizTypeEnum.SHOP.getCode());

        mqMessageSendUtils.sendMqMessage( SearchMqConstants.ES_SYNC_EXCHANGE, SearchMqConstants.ES_SYNC_BATCH_INSERT_ROUTING_KEY, request);
        mqMessageSendUtils.sendMqMessage( SearchMqConstants.MILVUS_SYNC_EXCHANGE, SearchMqConstants.MILVUS_SYNC_BATCH_INSERT_ROUTING_KEY, request);
    }

    /**
     * 批量更新店铺收藏数。
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
                baseMapper.updateStarCountBatch(batchMap);
            }
        } else {
            baseMapper.updateStarCountBatch(updateMap);
        }
        return true;
    }

    /**
     * 批量更新店铺粉丝数。
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
                baseMapper.updateFansCountBatch(batchMap);
            }
        } else {
            baseMapper.updateFansCountBatch(updateMap);
        }
        return true;
    }
    /**
     * 批量更新店铺销量。
     */
    @Override
    public Boolean updateSoldBatch(Map<Long, Integer> updateMap) {
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
                baseMapper.updateSoldBatch(batchMap);
            }
        } else {
            baseMapper.updateSoldBatch(updateMap);
        }
        return true;
    }

    /**
     * 同步店铺销量数据。
     */
    @Override
    public void syncSalesData() {
        log.info("starting shop sales data sync");
        String countKeyPrefix = SalesTypeEnum.SHOP_SALES.getCountKeyPrefix();
        String dirtyKey = SalesTypeEnum.SHOP_SALES.getDirtyKey();
        String tempKey = dirtyKey + ":TEMP";

        redisService.syncDataWithSnapshot("shop sales sync", countKeyPrefix, dirtyKey, tempKey,
                this::updateSoldBatch,
                updateMap -> enqueueIds(RedisConstants.SHOP_CALC_QUEUE_KEY, updateMap.keySet()));
        log.info("shop sales data sync finished");
    }


    /**
     * 将店铺ID加入待处理队列。
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
     * 批量更新店铺评价数。
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
                baseMapper.updateReviewCountBatch(batchMap);
            }
        } else {
            baseMapper.updateReviewCountBatch(updateMap);
        }
        return true;
    }

    /**
     * 更新店铺状态。
     */
    @Override
    public Boolean updateShopStatus(Long id, Integer status, String reason) {
        Shop shop = getById(id);
        String rejectReason = AuditStatusEnum.isRejected(status) ? reason : null;
        Integer businessStatus = null;
        if (Objects.equals(status, AuditStatusEnum.PASS.getCode())) {
            businessStatus = 1;
        } else if (AuditStatusEnum.isRejected(status)) {
            businessStatus = 2;
        }
        UpdateWrapper<Shop> uw = new UpdateWrapper<Shop>()
                .set("audit_status", status)
                .set("reject_reason", rejectReason)
                .eq("id", id);
        if (businessStatus != null) {
            uw.set("status", businessStatus);
        }
        boolean updated = update(uw);
        if (updated) {
            flashShopRedisCache(id);
            if (shop != null && shop.getTypeId() != null) {
                flashShopListRedisCache(shop.getTypeId());
            }
            if (Objects.equals(status, AuditStatusEnum.PASS.getCode())) {
                publish(new String[]{id.toString()});
                String hotRankKey = RedisConstants.SHOP_HOT_RANK_KEY;
                double score = shop != null && shop.getCreateTime() != null ? (double) shop.getCreateTime().getTime() : (double) System.currentTimeMillis();
                redisService.setCacheZSet(hotRankKey, id.toString(), score);
                redisService.setCacheSet(RedisConstants.SHOP_CALC_QUEUE_KEY, id.toString());
            } else {
                redisService.removeCacheZSetObject(RedisConstants.SHOP_HOT_RANK_KEY, id.toString());
                redisService.removeCacheSet(RedisConstants.SHOP_CALC_QUEUE_KEY, id.toString());
                ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
                contentSyncMessage.setId(id);
                contentSyncMessage.setIndexName(EsIndexNameConstants.SHOP_INDEX_NAME);
                contentSyncMessage.setType(GlobalBizTypeEnum.SHOP.getCode());
                mqMessageSendUtils.sendMqMessage(SearchMqConstants.ES_SYNC_EXCHANGE, SearchMqConstants.ES_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
                mqMessageSendUtils.sendMqMessage(SearchMqConstants.MILVUS_SYNC_EXCHANGE, SearchMqConstants.MILVUS_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
            }
        }
        return updated;
    }

    /**
     * 查询热门店铺排行榜。
     */
    @Override
    public List<ShopVO> getHotShopRank(Integer current, Integer size, Double x, Double y) {
        int pageNo = current == null || current < 1 ? 1 : current;
        int pageSize = size == null || size < 1 ? 10 : size;

        if (pageNo * pageSize > 100) {
            return Collections.emptyList();
        }

        List<ShopVO> resultList;

        Page<Long> longPage = zSetIdManager.pageIds(RedisConstants.SHOP_HOT_RANK_KEY, null, current, SystemConstants.MAX_PAGE_SIZE);
        List<Long> shopIdList = longPage.getRecords();

        if (CollUtil.isEmpty(shopIdList)&&current == 1) {
            log.info("shop hot rank zset is empty, loading shop ids from database");
            List<Shop> dbList = query()
                    .eq("status", 1)
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .orderByDesc("sold")
                    .orderByDesc("create_time")
                    .list();

            if (CollUtil.isNotEmpty(dbList)) {
                final List<Shop> finalDbList = dbList;
                executorService.execute(() -> {
                    log.info("异步初始化热门店铺排行榜 ZSet");
                    zSetIdManager.saveToZSet(RedisConstants.SHOP_HOT_RANK_KEY, finalDbList, Shop::getId, Shop::getCreateTime);

                    redisService.setCacheSet(RedisConstants.SHOP_CALC_QUEUE_KEY,
                            finalDbList.stream()
                                    .map(s -> String.valueOf(s.getId()))
                                    .collect(Collectors.toSet()));
                });

                int start = (pageNo - 1) * pageSize;
                if (dbList.size() > start) {
                    dbList = dbList.subList(start, Math.min(start + pageSize, dbList.size()));
                    shopIdList = dbList.stream().map(Shop::getId).collect(Collectors.toList());
                } else {
                    shopIdList = Collections.emptyList();
                }
            }
            resultList = CollUtil.isEmpty(shopIdList) ? Collections.emptyList() : getShopList(shopIdList);
        } else {
            resultList = getShopList(shopIdList);
        }

        resultList = resultList.stream()
                .filter(this::isVisibleShop)
                .collect(Collectors.toList());

        if (x != null && y != null && CollUtil.isNotEmpty(resultList)) {
            for (ShopVO shopVO : resultList) {
                if (shopVO.getX() != null && shopVO.getY() != null) {
                    double distance = SloppyMath.haversinMeters(y, x, shopVO.getY(), shopVO.getX());
                    shopVO.setDistance(distance);
                }
            }
        }
        return resultList;
    }

    /**
     * 根据用户ID查询店铺列表。
     */
    @Override
    public List<Shop> selectShopListByUserId(Long userId, Shop shop) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<Long> shopIds = remoteUserService.getShopIdsByUserId(userId);
        if (CollUtil.isEmpty(shopIds)) {
            return Collections.emptyList();
        }
        return shopMapper.selectShopListByIdsAndCondition(shopIds, shop == null ? new Shop() : shop);
    }

    /**
     * 按关键词搜索店铺。
     */
    @Override
    public List<ShopVO> searchShops(String keyword) {
        String trimmedKeyword = keyword == null ? null : keyword.trim();
        List<Shop> shops = query()
                .select("id", "name", "status", "audit_status")
                .like(StrUtil.isNotBlank(trimmedKeyword), "name", trimmedKeyword)
                .eq("status", 1)
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .list();
        return convertToShopVOList(shops);
    }
}

