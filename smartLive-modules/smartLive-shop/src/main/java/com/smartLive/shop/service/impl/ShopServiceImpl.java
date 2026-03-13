package com.smartLive.shop.service.impl;
import com.smartLive.common.core.constant.mq.SearchMqConstants;
import com.smartLive.common.core.constant.mq.AiAuditMqConstants;

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
import com.smartLive.system.api.RemoteUserService;
import com.smartLive.interaction.api.DTO.FollowDTO;
import com.smartLive.interaction.api.DTO.StarDTO;
import com.smartLive.shop.domain.ShopType;
import com.smartLive.shop.domain.VO.ShopVO;
import com.smartLive.shop.service.IShopTypeService;
import com.smartLive.common.redis.util.CacheClient;
import com.smartLive.common.redis.util.ZSetIdManager;
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
 * 店铺业务实现类
 * 
 * 核心职能：
 * 1. 维护店铺基础信息，并同步触发 Elasticsearch 与 Milvus 搜索引擎索引。
 * 2. 整合 Redis 多级缓存管理，解决高并发下的缓存穿透与缓存击穿（逻辑过期/互斥锁模式）。
 * 3. 联动互动模块接口，异步获取/更新店铺的收藏、关注、销量及评论数指标。
 * 4. 实现基于 Redis GEO 数据集的地理位置附近搜索与排行榜计算。
 *
 * @author smartLive
 * @date 2026-03-11
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
    private RedisMultiCacheManager redisMultiCacheManager;
    @Autowired
    private ZSetIdManager zSetIdManager;
    /**
     * 将Shop实体转换为ShopVO
     * @param shop Shop实体
     * @return ShopVO对象
     */
    private ShopVO convertToShopVO(Shop shop) {
        if (shop == null) {
            return null;
        }
        ShopVO shopVO = new ShopVO();
        BeanUtils.copyProperties(shop, shopVO);
        return shopVO;
    }

    private boolean isVisibleShop(Shop shop) {
        return shop != null
                && Objects.equals(shop.getStatus(), 1)
                && Objects.equals(shop.getAuditStatus(), AuditStatusEnum.PASS.getCode());
    }

    private boolean isVisibleShop(ShopVO shopVO) {
        return shopVO != null
                && Objects.equals(shopVO.getStatus(), 1)
                && Objects.equals(shopVO.getAuditStatus(), AuditStatusEnum.PASS.getCode());
    }

    /**
     * 将Shop列表转换为ShopVO列表
     * @param shopList Shop实体列表
     * @return ShopVO列表
     */
    private List<ShopVO> convertToShopVOList(List<Shop> shopList) {
        if (CollUtil.isEmpty(shopList)) {
            return new ArrayList<>();
        }
        return shopList.stream()
                .map(this::convertToShopVO)
                .collect(Collectors.toList());
    }
    /**
     * 查询店铺
     *
     * @param id 店铺主键
     * @return 店铺
     */
    @Override
    public Shop selectShopById(String id) {
        return shopMapper.selectShopById(id);
    }

    /**
     * 查询店铺列表
     *
     * @param shop 店铺
     * @return 店铺
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
     * 新增店铺
     *
     * @param shop 店铺
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertShop(Shop shop) {
        shop.setCreateTime(DateUtils.getNowDate());
        int i = shopMapper.insertShop(shop);
        if (i > 0) {
            Long userId = SecurityUtils.getUserId();
            if (userId == null) {
                throw new BusinessException("获取当前登录用户ID失败");
            }
            if (shop.getId() == null) {
                throw new BusinessException("店铺ID为空，无法保存用户店铺关系");
            }
            Boolean relationSaved = remoteUserService.addUserShopRelation(userId, shop.getId());
            if (!Boolean.TRUE.equals(relationSaved)) {
                throw new BusinessException("保存用户店铺关系失败");
            }
            flashShopListRedisCache(shop.getTypeId());
            //发送审核信息
            sendAuditMessage(shop);
        }
        return i;
    }

    /**
     * 修改店铺
     *
     * @param shop 店铺
     * @return 结果
     */
    @Override
    public int updateShop(Shop shop) {
        Shop oldShop = shop.getId() == null ? null : shopMapper.selectById(shop.getId());
        shop.setUpdateTime(DateUtils.getNowDate());
        int i = shopMapper.updateShop(shop);
        if(i > 0){
            Long shopId = shop.getId();
            flashShopRedisCache(shopId);
            if (oldShop != null && oldShop.getTypeId() != null) {
                flashShopListRedisCache(oldShop.getTypeId());
            }
            if (shop.getTypeId() != null && (oldShop == null || !Objects.equals(oldShop.getTypeId(), shop.getTypeId()))) {
                flashShopListRedisCache(shop.getTypeId());
            }
            //更新es数据
            publish(new String[]{shopId.toString()});
            //发送审核信息
            sendAuditMessage(shop);
        }
        return i;
    }

    /**
     * 实现分页删除店铺逻辑
     * 1. 物理删除数据库记录。
     * 2. 异步发送 MQ 消息同步清除搜索引擎 (ES/Milvus) 索引。
     * 3. 立即清除 Redis 分类列表缓存及详情缓存。
     *
     * @param ids 需要删除的店铺主键集合
     * @return 删除行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteShopByIds(String[] ids) {
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
                    throw new BusinessException("删除用户店铺关系失败");
                }
            }
            for (String id : ids) {
                executorService.submit(() -> {
                    log.info("线程{}，开始删除店铺索引{}", Thread.currentThread().getName(), id);
                    Long shopId;
                    try {
                        shopId = Long.valueOf(id);
                    } catch (NumberFormatException e) {
                        log.warn("删除店铺同步消息时店铺ID格式非法: {}", id);
                        return;
                    }
                    ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
                    contentSyncMessage.setId(shopId);
                    contentSyncMessage.setIndexName(EsIndexNameConstants.SHOP_INDEX_NAME);
                    contentSyncMessage.setType(GlobalBizTypeEnum.SHOP.getCode());
                    //发起rabbitMq消息删除es数据
                    mqMessageSendUtils.sendMqMessage(SearchMqConstants.ES_SYNC_EXCHANGE, SearchMqConstants.ES_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
                    //发起rabbitmq消息删除milvus数据
                    mqMessageSendUtils.sendMqMessage(SearchMqConstants.MILVUS_SYNC_EXCHANGE, SearchMqConstants.MILVUS_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
                });
            }
            Arrays.stream(ids).forEach(shopId -> {
                try {
                    flashShopRedisCache(Long.valueOf(shopId));
                } catch (NumberFormatException e) {
                    log.warn("删除店铺缓存时店铺ID格式非法: {}", shopId);
                }
            });
            shops.stream()
                    .map(Shop::getTypeId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet())
                    .forEach(this::flashShopListRedisCache);
        }
        return i;
    }

    /**
     * 删除店铺信息
     *
     * @param id 店铺主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteShopById(String id) {
        Shop shop = shopMapper.selectShopById(id);
        int i = shopMapper.deleteShopById(id);
        if (i > 0) {
            Long shopId = null;
            if (shop != null && shop.getId() != null) {
                shopId = shop.getId();
                flashShopRedisCache(shop.getId());
            } else {
                try {
                    shopId = Long.valueOf(id);
                    flashShopRedisCache(shopId);
                } catch (NumberFormatException e) {
                    log.warn("删除店铺缓存时店铺ID格式非法: {}", id);
                }
            }

            if (shopId != null) {
                Boolean relationDeleted = remoteUserService.deleteUserShopRelationByShopId(shopId);
                if (!Boolean.TRUE.equals(relationDeleted)) {
                    throw new BusinessException("删除用户店铺关系失败");
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
     * 根据 ID 获取商铺详情
     * 采用“逻辑过期”结合“空值缓存”的综合策略解决缓存击穿与穿透问题。
     * 内部还会联动互动模块查询当前用户是否收藏或关注了该店铺。
     *
     * @param id 商铺 ID
     * @return 增强后的商铺 VO
     */
    @Override
    public ShopVO queryById(Long id) {
        //解决缓存穿透
//        Shop shop = queryWithPassThrough(id);
        //互斥锁解决缓存击穿
//        Shop shop = queryWithMutex(id);
        //逻辑过期来解决缓存击穿
//        Shop shop = queryWithLogicalExpire(id);
        // 利用 CacheClient 工具类封装逻辑过期与防穿透逻辑
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
        //逻辑过期解决缓存击穿 使用工具类CacheClient
//        Shop shop = cacheClient.queryWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY, id, Shop.class,this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if (shop == null) {
            return null;
        }
        if (!isVisibleShop(shop)) {
            return null;
        }
        ShopVO shopVO = convertToShopVO(shop);
        // 渲染当前用户的互动状态：收藏与关注
        isShopStared(shopVO);
        isShopFollowed(shopVO);
        return shopVO;
    }

    /**
     * 判断当前用户是否已经收藏店铺
     *
     * @param shopVO 店铺VO（设置isStared属性）
     */
    private void isShopStared(ShopVO shopVO) {
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            //未登录,不用查询是否收藏
            shopVO.setIsStared(false);
            return;
        }
        //获取当前登录用户
        Long userId = user.getId();
        StarDTO starDTO = new StarDTO();
        starDTO.setUserId(userId);
        starDTO.setSourceId(shopVO.getId());
        starDTO.setSourceType(GlobalBizTypeEnum.SHOP.getCode());
        //判断当前用户是否已经收藏
        Boolean isStared = remoteStarService.isStar(starDTO);
        shopVO.setIsStared(isStared);
    }
    /**
     * 判断当前用户是否已经关注店铺
     *
     * @param shopVO 店铺VO（设置isFollowed属性）
     */
    private void isShopFollowed(ShopVO shopVO) {
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            //未登录,不用查询是否关注
            shopVO.setIsFollowed(false);
            return;
        }
        //获取当前登录用户
        Long userId = user.getId();
        FollowDTO followDTO = new FollowDTO();
        followDTO.setUserId(userId);
        followDTO.setSourceId(shopVO.getId());
        followDTO.setSourceType(GlobalBizTypeEnum.SHOP.getCode());
        Boolean isFollowed = remoteFollowService.isFollowed(followDTO);
        shopVO.setIsFollowed(isFollowed);
    }
    /**
     * 缓存穿透解决方案：查询店铺并缓存空值防止穿透
     *
     * @param id 店铺ID
     * @return 店铺VO，不存在返回null
     */
    public ShopVO queryWithPassThrough(Long id) {

        //从缓存里获取商铺数据
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        String shopJson = redisService.getCacheObject(key);
        //判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            //存在，直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return convertToShopVO(shop);
        }
        //判断命中的是否是空值
        if (shopJson != null) {
            return null;
        }
        Shop shop = this.getById(id);
        if (shop == null) {
            //防止缓存穿透,将空值存入redis
            redisService.setCacheObject(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //存入redis
        redisService.setCacheObject(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return convertToShopVO(shop); // Convert Shop to ShopVO before returning

    }

    //创建缓存线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 逻辑过期策略方案：
     * 用于解决极高并发下的“缓存击穿”。即使缓存过期，先返回旧数据，
     * 同时开启后台线程异步查询数据库并更新缓存，从而保证系统吞吐量。
     *
     * @param id 店铺 ID
     * @return 包含逻辑过期数据的店铺对象
     */
    public Shop queryWithLogicalExpire(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        //从缓存里获取商铺缓存
        String shopJson = redisService.getCacheObject(key);
        //判断是否存在
        if (StrUtil.isBlank(shopJson)) {
            //不存在，直接返回空
            return null;
        }
        //把json转换成对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        //判断是否过期
        if (redisData.getExpireTime().isAfter(LocalDateTime.now())) {
            //未过期，直接返回数据
            return shop;
        }
        //TODO 过期，缓存重建
        //获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        if (isLock) {
            //取锁成功，开启独立线程,进行缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    //重建缓存
                    this.saveHotShopRedis(id, 20L);
                } catch (Exception e) {

                } finally {
                    //释放锁
                    unLock(lockKey);
                }
            });
        }
        //存入redis
        return shop;

    }


    /**
     * 互斥锁解决缓存击穿：加锁防止并发查库
     *
     * @param id 店铺ID
     * @return 店铺实体
     */
    public Shop queryWithMutex(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        //从缓存里获取商铺缓存
        String shopJson = redisService.getCacheObject(key);
        //判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            //存在，直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        //判断命中的是否是空值
        if (shopJson != null) {
            return null;
        }

        // TODO 实现缓存重建
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            //获取互斥锁
            boolean isLock = tryLock(lockKey);
            //判断是否获取成功
            if (!isLock) {
                //获取锁失败，休眠重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //获取锁成功，实现缓存重建
            shop = this.getById(id);
            //模拟重建延时
//            Thread.sleep(5000);
            if (shop == null) {
                //防止缓存穿透,将空值存入redis
                redisService.setCacheObject(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            //存入redis
            redisService.setCacheObject(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
        } finally {
            unLock(lockKey);
        }
        //释放互斥锁
        unLock(lockKey);
        return shop;

    }

    /**
     * 尝试获取Redis分布式锁
     *
     * @param key 锁的键名
     * @return 是否获取成功
     */
    private boolean tryLock(String key) {
        boolean flag = redisService.setCacheObjectIfAbsent(key, "1", RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放Redis分布式锁
     *
     * @param key 锁的键名
     */
    private void unLock(String key) {
        redisService.deleteObject(key);
    }

    /**
     * 保存热点店铺数据到Redis（带逻辑过期时间）
     *
     * @param id            店铺ID
     * @param expireSeconds 逻辑过期时间（秒）
     * @throws InterruptedException 模拟延时中断异常
     */
    public void saveHotShopRedis(Long id, Long expireSeconds) throws InterruptedException {
        //查询店铺数据
        Shop shop = this.getById(id);
        Thread.sleep(2000);
        //封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //写入redis
        redisService.setCacheObject(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 根据商铺名称查询商铺信息
     *
     * @param shopName 商铺名称
     * @return 商铺详情
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
     * 多维度条件搜索商铺逻辑
     * 支持分类过滤、关键字模糊匹配（名称/地址）以及地理坐标 (X,Y) 距离排序。
     *
     * @param shop 包含搜索条件的实体 (包含用户坐标)
     * @return 根据距离从近到远排序的商铺列表
     */
    @Override
    public List<ShopVO> getShopByCondition(Shop shop) {
        QueryWrapper<Shop> wrapper = new QueryWrapper<>();
        // 利用 MySQL 函数计算球面距离
        String distanceSql = "ST_Distance_Sphere(point(x, y), point(" + shop.getX() + ", " + shop.getY() + ")) as distance";
        wrapper.select("*, " + distanceSql);
        wrapper.eq("status", 1);
        wrapper.eq("audit_status", AuditStatusEnum.PASS.getCode());
        // 1. 分类条件
        if (shop.getTypeId() != null) {
            wrapper.eq("type_id", shop.getTypeId());
        }

        // 2. 文本搜索条件
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

        // 3. 地理位置条件
        if (shop.getX() != null && shop.getY() != null ) {
            wrapper.apply("ST_Distance_Sphere(point(x, y), point({0}, {1})) <= {2}",
                    shop.getX(), shop.getY(), 200000);
        }
        wrapper .orderByAsc("distance");
        return convertToShopVOList(list(wrapper));
    }

    /**
     * 根据商铺id列表查询商铺信息列表
     *
     * @param ids 商铺id列表
     * @return 商铺列表
     */
    @Override
    public List<ShopVO> getShopList(List<Long> ids) {
        List<ShopVO> shopList = redisMultiCacheManager.queryBatchWithCache(
                RedisConstants.CACHE_SHOP_KEY,
                RedisConstants.LOCK_SHOP_KEY,
                ids,
                ShopVO.class,
                missingIds -> {
                    // 3. 填查库逻辑 (Lambda表达式)
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
        return shopList.stream()
                .filter(this::isVisibleShop)
                .collect(Collectors.toList());
    }

    /**
     * 获取商铺总数
     *
     * @return 商铺总数
     */
    @Override
    public Integer getShopTotal() {
        return query().count().intValue();
    }

    /**
     * 获取最近商铺
     *
     * @param limit 获取数量
     * @return 最近商铺
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
     * 手动触发商铺全量缓存刷新
     * 1. 清除并重建基于分类的商铺列表 Redis 缓存。
     * 2. 清除并重建基于 Redis GEO 索引的商铺坐标缓存（用于附近搜索）。
     *
     * @return 刷新任务执行结果
     */
    @Override
    public String flushCache() {
        //缓存店铺数据
        String key = RedisConstants.CACHE_SHOP_lIST_KEY+"*";
        //删除所有店铺缓存
        redisService.deleteObject(redisService.keys(key));
        List<ShopType> shopTypeList = shopTypeService.list();
        shopTypeList.forEach(shopType -> {
            List<Shop> shopList = query()
                    .eq("type_id", shopType.getId())
                    .eq("status", 1)
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .list();
            //缓存
            redisService.setCacheObject(RedisConstants.CACHE_SHOP_lIST_KEY+shopType.getId(), shopList);
        });


        //缓存店铺坐标数据
        List<Shop> list = query()
                .eq("status", 1)
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .list();
        //删除所有店铺的坐标缓存
        redisService.deleteObject(redisService.keys(RedisConstants.SHOP_GEO_KEY+"*"));
        //把店铺分组 按照typeId分组 id一致放到一个集合
        Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        //分批放入redis里面
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            //获取类型id
            Long typeId = entry.getKey();
            String shopGeoKey = RedisConstants.SHOP_GEO_KEY + typeId;
            //获取同类型的店铺
            List<Shop> shopList = entry.getValue();
            //方法一 循环写入
            for (Shop shop : shopList) {
                //写入redis  GEOADD key 经度 纬度 member
                redisService.addCacheGeoLocation(shopGeoKey, shop.getX(), shop.getY(), shop.getId().toString());
            }
        }
        return "刷新成功";
    }
    /**
     * 发送审核消息到MQ
     *
     * @param shop 店铺实体
     */
    private void sendAuditMessage(Shop shop) {
        AuditMessage auditMessage = AuditMessage.builder()
                .bizId(shop.getId())
                .bizType(GlobalBizTypeEnum.SHOP.getCode())
                .submitterId(SecurityContextHolder.getUserId())
                .auditContent(BeanUtil.beanToMap(shop))
                .createTime(shop.getCreateTime())
                .build();
        mqMessageSendUtils.sendMqMessage( AiAuditMqConstants.AUDIT_DIRECT_EXCHANGE,AiAuditMqConstants.AUDIT_ROUTING_KEY, auditMessage);
    }
    /**
     * 清空指定分类的店铺列表缓存
     *
     * @param typeId 分类ID
     */
    private void flashShopListRedisCache(Long typeId) {
        //清空缓存
        redisService.deleteObject(RedisConstants.CACHE_SHOP_lIST_KEY+typeId);
    }

    /**
     * 清空指定店铺的详情缓存
     *
     * @param id 店铺ID
     */
    private void flashShopRedisCache(Long id){
        redisService.deleteObject(RedisConstants.CACHE_SHOP_KEY+id);
    }

    /**
     * 全量发布店铺数据至搜索引擎
     * 采用分页查询数据库并利用线程池异步发送 MQ 批量同步消息，确保 ES 与 Milvus 数据最新。
     *
     * @return 发布任务启动说明
     */
    @Override
    public String allPublish() {
        int page = PageConstants.PAGE_NUMBER;
        int pageSize = PageConstants.ES_PAGE_SIZE; // 每页50条
        while (true) {
            // 分页查询
            List<Shop> shops = query()
                    .eq("status", 1)
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .page(new Page<>(page, pageSize))
                    .getRecords();
            if (shops.isEmpty()) {
                break;
            }
            int finalPage = page;
            //使用多线程来处理
            executorService.submit(()->{
                List<ShopVO> voList = convertToShopVOList(shops);
                voList.forEach(vo -> vo.setLocation(vo.getY() + "," + vo.getX()));
                // 发送批量消息
                sendShopBatchMessage(voList);
                log.info("线程{}，发送第 {} 页，{} 条数据",Thread.currentThread().getName(),finalPage, shops.size());
            });
            page++;
        }
        return "数据发布完成";
    }


    /**
     * 批量发布店铺至ES和Milvus索引
     *
     * @param ids 店铺ID数组
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
            log.info("线程{}，开始批量发布店铺：{}", Thread.currentThread().getName(), idList);
            // Batch query
            List<Shop> shops = query()
                    .in("id", idList)
                    .eq("status", 1)
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .list();
            if (CollUtil.isNotEmpty(shops)) {
                List<ShopVO> voList = convertToShopVOList(shops);
                // Set location
                voList.forEach(vo -> vo.setLocation(vo.getY() + "," + vo.getX()));
                // Batch send message
                sendShopBatchMessage(voList);
            }
        });
        return "发布成功";
    }

    /**
     * 批量发送ES和Milvus同步消息
     *
     * @param shops 店铺列表
     */
    private void sendShopBatchMessage(List<?> shops) {
        if (CollUtil.isEmpty(shops)) {
            return;
        }
        ContentBatchSyncMessage request = new ContentBatchSyncMessage();
        request.setIndexName(EsIndexNameConstants.SHOP_INDEX_NAME);
        request.setData(shops);
        request.setType(GlobalBizTypeEnum.SHOP.getCode());
        
        // 发送rabbitmq消息数据插入es
        mqMessageSendUtils.sendMqMessage( SearchMqConstants.ES_SYNC_EXCHANGE, SearchMqConstants.ES_SYNC_BATCH_INSERT_ROUTING_KEY, request);
        // 发送rabbitmq消息数据插入Milvus
        mqMessageSendUtils.sendMqMessage( SearchMqConstants.MILVUS_SYNC_EXCHANGE, SearchMqConstants.MILVUS_SYNC_BATCH_INSERT_ROUTING_KEY, request);
    }

    /**
     * 批量更新商铺收藏数 (统计逻辑)
     * 1. 采用分包批处理模式，防止 SQL 参数过长。
     * 2. 更新后同步刷新 Redis 详情缓存及全量热度缓存。
     *
     * @param updateMap 店铺 ID -> 收藏增量
     * @return 执行结果
     */
    @Override
    public Boolean updateStarCountBatch(Map<Long, Integer> updateMap) {
        if (CollUtil.isEmpty(updateMap)) {
            return false;
        }

        // 建议：如果数量特别大(超过500)，建议分批，防止 SQL 语句超长报错
        // 如果你确信每 30秒 的点赞更新量不会导致 SQL 超过 4MB，可以直接调 baseMapper
        if (updateMap.size() > 500) {
            // 分批逻辑 (每500条提交一次)
            List<List<Long>> partition = ListUtil.partition(new ArrayList<>(updateMap.keySet()), 500);
            for (List<Long> batchKeys : partition) {
                Map<Long, Integer> batchMap = new HashMap<>();
                for (Long key : batchKeys) {
                    batchMap.put(key, updateMap.get(key));
                }
                baseMapper.updateStarCountBatch(batchMap);
            }
        } else {
            // 数量少直接执行
            baseMapper.updateStarCountBatch(updateMap);
        }
        updateMap.keySet().forEach(this::flashShopRedisCache);
        flushCache();
        return true;
    }

    /**
     * 批量更新商铺评论数
     *
     * @param updateMap 商铺id和评论数
     * @return 批量更新结果
     */
    /**
     * 批量更新商铺粉丝数
     *
     * @param updateMap 商铺id和粉丝数
     * @return 批量更新结果
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
        updateMap.keySet().forEach(this::flashShopRedisCache);
        flushCache();
        return true;
    }
    /**
     * 批量更新店铺销量
     *
     * @param updateMap 店铺ID与销量的映射
     * @return 更新结果
     */
    @Override
    public Boolean updateSoldBatch(Map<Long, Integer> updateMap) {
        if (CollUtil.isEmpty(updateMap)) {
            return false;
        }
        // 分批分发，防止后端 SQL 语句过载
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
        // 刷新 Redis 详情缓存
        updateMap.keySet().forEach(this::flashShopRedisCache);
        // 刷新列表缓存（销量变更会影响排序）
        flushCache();
        return true;
    }

    /**
     * 定时任务执逻辑：同步店铺销量数据从 Redis 到数据库
     */
    @Override
    public void syncSalesData() {
        log.info("开始定时任务：同步店铺销量数据");
        String countKeyPrefix = SalesTypeEnum.SHOP_SALES.getCountKeyPrefix();
        String dirtyKey = SalesTypeEnum.SHOP_SALES.getDirtyKey();
        String tempKey = dirtyKey + ":TEMP";

        // 执行同步逻辑，完成后更新排行榜权重队列
        redisService.syncDataWithSnapshot("店铺销量", countKeyPrefix, dirtyKey, tempKey,
                this::updateSoldBatch,
                updateMap -> enqueueIds(RedisConstants.SHOP_CALC_QUEUE_KEY, updateMap.keySet()));
        log.info("定时任务：同步店铺销量数据完成");
    }


    /**
     * 推送 ID 到算分队列，用于更新热门店铺排行榜
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

    @Override
    public Boolean updateReviewCountBatch(Map<Long, Integer> updateMap) {
        if (CollUtil.isEmpty(updateMap)) {
            return false;
        }

        // 建议：如果数量特别大(超过500)，建议分批，防止 SQL 语句超长报错
        // 如果你确信每 30秒 的点赞更新量不会导致 SQL 超过 4MB，可以直接调 baseMapper
        if (updateMap.size() > 500) {
            // 分批逻辑 (每500条提交一次)
            List<List<Long>> partition = ListUtil.partition(new ArrayList<>(updateMap.keySet()), 500);
            for (List<Long> batchKeys : partition) {
                Map<Long, Integer> batchMap = new HashMap<>();
                for (Long key : batchKeys) {
                    batchMap.put(key, updateMap.get(key));
                }
                baseMapper.updateReviewCountBatch(batchMap);
            }
        } else {
            // 数量少直接执行
            baseMapper.updateReviewCountBatch(updateMap);
        }
        updateMap.keySet().forEach(this::flashShopRedisCache);
        flushCache();
        return true;
    }

    /**
     * 更新店铺状态
     *
     * @param id     店铺ID
     * @param status 状态
     * @return 结果
     */
    @Override
    public Boolean updateShopStatus(Long id, Integer status, String reason) {
        Shop shop = getById(id);
        // 拒绝时写入拒绝原因，通过时清空拒绝原因
        String rejectReason = AuditStatusEnum.isRejected(status) ? reason : null;
        Integer businessStatus = null;
        // 店铺业务状态：1=启用，2=停用
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
     * 获取热门店铺排行榜（大一统分页接口）
     * 首页调用：传 current=1, size=10
     * 榜单页调用：传 current=n, size=10
     *
     * @param current 页码
     * @param size    每页数量
     * @param x       用户经度
     * @param y       用户纬度
     * @return 热门店铺列表
     */
    @Override
    public List<ShopVO> getHotShopRank(Integer current, Integer size, Double x, Double y) {
        int pageNo = current == null || current < 1 ? 1 : current;
        int pageSize = size == null || size < 1 ? 10 : size;

        // 1. 防御性拦截：最多只给看前 100 名
        if (pageNo * pageSize > 100) {
            return Collections.emptyList();
        }

        List<ShopVO> resultList;

        // 从最新的 ZSet 热度排行榜中获取博客 ID 极其分页数据
        Page<Long> longPage = zSetIdManager.pageIds(RedisConstants.SHOP_HOT_RANK_KEY, null, current, SystemConstants.MAX_PAGE_SIZE);
        List<Long> shopIdList = longPage.getRecords();

        if (CollUtil.isEmpty(shopIdList)&&current == 1) {
            // ZSet 击穿或尚无数据时的兜底：查出全量数据写入 ZSet，再手动分页返回
            log.info("店铺热榜 ZSet 为空，走数据库兜底查询");
            List<Shop> dbList = query()
                    .eq("status", 1)
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .orderByDesc("sold")
                    .orderByDesc("create_time")
                    .list();

            if (CollUtil.isNotEmpty(dbList)) {
                final List<Shop> finalDbList = dbList;
                executorService.execute(() -> {
                    log.info("重建店铺热榜 ZSet");
                    zSetIdManager.saveToZSet(RedisConstants.SHOP_HOT_RANK_KEY, finalDbList, Shop::getId, Shop::getCreateTime);

                    // 推入计算队列，等候定时任务处理真正的衰减和融合热度分
                    redisService.setCacheSet(RedisConstants.SHOP_CALC_QUEUE_KEY,
                            finalDbList.stream()
                                    .map(s -> String.valueOf(s.getId()))
                                    .collect(Collectors.toSet()));
                });

                // 手动分页截取当前页数据
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
            // 4. 批量查询店铺详情（复用缓存批量查询）
            resultList = getShopList(shopIdList);
        }

        resultList = resultList.stream()
                .filter(this::isVisibleShop)
                .collect(Collectors.toList());

        // 计算距离
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
     * 根据用户id查询所属店铺
     *
     * @param userId
     * @param shop
     * @return
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
}
