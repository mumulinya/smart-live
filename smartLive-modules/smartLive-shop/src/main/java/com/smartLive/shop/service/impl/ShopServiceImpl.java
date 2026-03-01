package com.smartLive.shop.service.impl;
import com.smartLive.common.core.constant.mq.SearchMqConstants;
import com.smartLive.common.core.constant.mq.AiAuditMqConstants;

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
import com.smartLive.common.rabbitmq.domain.AuditMessage;
import com.smartLive.common.rabbitmq.domain.ContentBatchSyncMessage;
import com.smartLive.common.rabbitmq.domain.ContentSyncMessage;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.utils.StringUtils;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.common.redis.util.RedisMultiCacheManager;
import com.smartLive.interaction.api.RemoteFollowService;
import com.smartLive.interaction.api.RemoteStarService;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.smartLive.shop.mapper.ShopMapper;
import com.smartLive.shop.domain.Shop;
import com.smartLive.shop.service.IShopService;
import jakarta.annotation.Resource;

/**
 * 店铺Service业务层处理
 *
 * @author mumulin
 * @date 2025-09-21
 */
@Service
@Slf4j
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Autowired
    private ShopMapper shopMapper;
    @Autowired
    private IShopTypeService shopTypeService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private RemoteStarService remoteStarService;
    @Autowired
    RemoteFollowService remoteFollowService;
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
        return shopMapper.selectShopList(shop);
    }

    /**
     * 新增店铺
     *
     * @param shop 店铺
     * @return 结果
     */
    @Override
    public int insertShop(Shop shop) {
        shop.setCreateTime(DateUtils.getNowDate());
        int i = shopMapper.insertShop(shop);
        if(i > 0){
            flashShopListRedisCache(shop.getTypeId());
            publish(new String[]{shop.getId().toString()});
            //发送审核信息
            sendAuditMessage(shop);
        }
        return i ;
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
     * 批量删除店铺
     *
     * @param ids 需要删除的店铺主键
     * @return 结果
     */
    @Override
    public int deleteShopByIds(String[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        List<Shop> shops = Arrays.stream(ids)
                .map(shopMapper::selectShopById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        int i = shopMapper.deleteShopByIds(ids);
        if (i > 0) {
            for (String id : ids) {
                executorService.submit(()->{
                    log.info("线程{}，开始删除店铺{}", Thread.currentThread().getName(), id);
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
                    //发起rabbitMq信息删除es数据
                    MqMessageSendUtils.sendMqMessage(rabbitTemplate, SearchMqConstants.ES_EXCHANGE, SearchMqConstants.ES_ROUTING_DELETE, contentSyncMessage);
                    //发起rabbitmq信息删除milvus数据
                    MqMessageSendUtils.sendMqMessage(rabbitTemplate, SearchMqConstants.MILVUS_EXCHANGE, SearchMqConstants.MILVUS_ROUTING_DELETE, contentSyncMessage);
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
    public int deleteShopById(String id) {
        Shop shop = shopMapper.selectShopById(id);
        int i = shopMapper.deleteShopById(id);
        if (i > 0) {
            if (shop != null && shop.getId() != null) {
                flashShopRedisCache(shop.getId());
            } else {
                try {
                    flashShopRedisCache(Long.valueOf(id));
                } catch (NumberFormatException e) {
                    log.warn("删除店铺缓存时店铺ID格式非法: {}", id);
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
     * 根据id查询商铺信息
     *
     * @param id 商铺id
     * @return 商铺详情数据
     */
    @Override
    public ShopVO queryById(Long id) {
        //解决缓存穿透
//        Shop shop = queryWithPassThrough(id);
        //互斥锁解决缓存击穿
//        Shop shop = queryWithMutex(id);
        //逻辑过期来解决缓存击穿
//        Shop shop = queryWithLogicalExpire(id);
        //缓存穿透,使用工具类CacheClient
        Shop shop = cacheClient.queryWithLogicalExpireAndPassThrough(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //逻辑过期解决缓存击穿 使用工具类CacheClient
//        Shop shop = cacheClient.queryWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY, id, Shop.class,this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if (shop == null) {
            return null;
        }
        //是否收藏
        isShopStared(shop);
        //是否关注
        isShopFollowed(shop);
        return convertToShopVO(shop);
    }

    /**
     * 判断当前用户是否已经收藏店铺
     *
     * @param shop 店铺实体（设置isStared属性）
     */
    private void isShopStared(Shop shop) {
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            //未登录,不用查询是否点赞
            shop.setIsStared(false);
            return;
        }
        //获取当前登录用户
        Long userId = user.getId();
        StarDTO starDTO = new StarDTO();
        starDTO.setUserId(userId);
        starDTO.setSourceId(shop.getId());
        starDTO.setSourceType(GlobalBizTypeEnum.SHOP.getCode());
        //判断当前用户是否已经收藏
        Boolean isStared = remoteStarService.isStar(starDTO);
        shop.setIsStared(isStared);
    }
    /**
     * 判断当前用户是否已经关注店铺
     *
     * @param shop 店铺实体（设置isFollowed属性）
     */
    private void isShopFollowed(Shop shop) {
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            //未登录,不用查询是否点赞
            shop.setIsFollowed(false);
            return;
        }
        //获取当前登录用户
        Long userId = user.getId();
        FollowDTO followDTO = new FollowDTO();
        followDTO.setUserId(userId);
        followDTO.setSourceId(shop.getId());
        followDTO.setSourceType(GlobalBizTypeEnum.SHOP.getCode());
        Boolean isFollowed = remoteFollowService.isFollowed(followDTO);
        shop.setIsFollowed(isFollowed);
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
     * 逻辑过期解决缓存击穿：不删缓存，返回旧数据并异步重建
     *
     * @param id 店铺ID
     * @return 店铺实体，缓存不存在返回null
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
        Shop shop = query().eq("name", shopName).one();
        return convertToShopVO(shop);
    }
    /**
     * 根据条件查询商铺信息
     *
     * @param shop 搜索条件
     * @return 搜索结果
     */
    @Override
    public List<ShopVO> getShopByCondition(Shop shop) {
        QueryWrapper<Shop> wrapper = new QueryWrapper<>();
        String distanceSql = "ST_Distance_Sphere(point(x, y), point(" + shop.getX() + ", " + shop.getY() + ")) as distance";
        wrapper.select("*, " + distanceSql);
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
       return redisMultiCacheManager.queryBatchWithCache(
                RedisConstants.CACHE_SHOP_KEY,
                ids,
                ShopVO.class,
                missingIds -> {
                    // 3. 填查库逻辑 (Lambda表达式)
                    List<Shop> shops = shopMapper.selectBatchIds(missingIds);
                    return convertToShopVOList(shops);
                },
                ShopVO::getId,
                RedisConstants.CACHE_SHOP_TTL,
                TimeUnit.MINUTES
        );
//        return convertToShopVOList(orderList);
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
        return convertToShopVOList(query().orderByDesc("create_time").last("limit " + limit).list());
    }

    /**
     * 刷新商铺缓存
     *
     * @return 刷新结果
     */
    @Override
    public String flushCache() {
        //缓存店铺数据
        String key = RedisConstants.CACHE_SHOP_lIST_KEY+"*";
        //删除所有店铺缓存
        redisService.deleteObject(redisService.keys(key));
        List<ShopType> shopTypeList = shopTypeService.list();
        shopTypeList.forEach(shopType -> {
            List<Shop> shopList = query().eq("type_id", shopType.getId()).list();
            //缓存
            redisService.setCacheObject(RedisConstants.CACHE_SHOP_lIST_KEY+shopType.getId(), shopList);
        });


        //缓存店铺坐标数据
        List<Shop> list = list();
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
        MqMessageSendUtils.sendMqMessage(rabbitTemplate, AiAuditMqConstants.AUDIT_EXCHANGE_NAME,AiAuditMqConstants.AUDIT_ROUTING_KEY, auditMessage);
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
     * 全部发布店铺
     *
     * @return 全部发布结果
     */
    @Override
    public String allPublish() {
        int page = PageConstants.PAGE_NUMBER;
        int pageSize = PageConstants.ES_PAGE_SIZE; // 每页50条
        while (true) {
            // 分页查询
            List<Shop> shops = query()
                    .page(new Page<>(page, pageSize))
                    .getRecords();
            if (shops.isEmpty()) {
                break;
            }
            int finalPage = page;
            //使用多线程来处理
            executorService.submit(()->{
                shops.forEach(
                        shop -> {
                            shop.setLocation(shop.getY() + "," + shop.getX());
                        }
                );
                // 发送批量消息
                sendShopBatchMessage(shops);
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
            List<Shop> shops = query().in("id", idList).list();
            if (CollUtil.isNotEmpty(shops)) {
                // Set location
                shops.forEach(shop -> shop.setLocation(shop.getY() + "," + shop.getX()));
                // Batch send message
                sendShopBatchMessage(shops);
            }
        });
        return "发布成功";
    }

    /**
     * 批量发送ES和Milvus同步消息
     *
     * @param shops 店铺列表
     */
    private void sendShopBatchMessage(List<Shop> shops) {
        if (CollUtil.isEmpty(shops)) {
            return;
        }
        ContentBatchSyncMessage request = new ContentBatchSyncMessage();
        request.setIndexName(EsIndexNameConstants.SHOP_INDEX_NAME);
        request.setData(shops);
        request.setType(GlobalBizTypeEnum.SHOP.getCode());
        
        // 发送rabbitmq消息数据插入es
        MqMessageSendUtils.sendMqMessage(rabbitTemplate, SearchMqConstants.ES_EXCHANGE, SearchMqConstants.ES_ROUTING_BATCH_INSERT, request);
        // 发送rabbitmq消息数据插入Milvus
        MqMessageSendUtils.sendMqMessage(rabbitTemplate, SearchMqConstants.MILVUS_EXCHANGE, SearchMqConstants.MILVUS_ROUTING_BATCH_INSERT, request);
    }

    /**
     * 批量更新商铺收藏数
     *
     * @param updateMap 商铺id和收藏数
     * @return 更新结果
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
    public Boolean updateShopStatus(Long id, Integer status) {
        Shop shop = getById(id);
        boolean updated = update(new UpdateWrapper<Shop>().set("status", status).eq("id", id));
        if (updated) {
            flashShopRedisCache(id);
            if (shop != null && shop.getTypeId() != null) {
                flashShopListRedisCache(shop.getTypeId());
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

        if (CollUtil.isEmpty(shopIdList)) {
            // ZSet 击穿或尚无数据时的兜底：查出全量数据写入 ZSet，再手动分页返回
            log.info("店铺热榜 ZSet 为空，走数据库兜底查询");
            List<Shop> dbList = query()
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

        // 计算距离
        if (x != null && y != null && CollUtil.isNotEmpty(resultList)) {
            for (ShopVO shopVO : resultList) {
                if (shopVO.getX() != null && shopVO.getY() != null) {
                    double distance = SloppyMath.haversinMeters(y, x, shopVO.getY(), shopVO.getX());
                    shopVO.setDistance(distance);
                }
            }
        }

        // 从 Redis ZSet 中批量获取热度评分（Pipeline 一次往返）
        if (CollUtil.isNotEmpty(resultList)) {
            List<String> memberIds = resultList.stream()
                    .map(s -> String.valueOf(s.getId()))
                    .collect(Collectors.toList());
            List<Double> scores = redisService.getCacheZSetScoreBatch(RedisConstants.SHOP_HOT_RANK_KEY, memberIds);
            log.info("获取店铺 {} 的热度评分为{}",memberIds,scores);
            for (int i = 0; i < resultList.size(); i++) {
                resultList.get(i).setHotScore(scores.get(i));
            }
        }
        return resultList;
    }
}
