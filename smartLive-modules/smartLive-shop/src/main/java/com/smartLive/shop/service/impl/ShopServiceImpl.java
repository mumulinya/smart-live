package com.smartLive.shop.service.impl;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.*;
import com.smartLive.common.core.domain.EsBatchInsertRequest;
import com.smartLive.common.core.domain.EsInsertRequest;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.domain.shop.ShopDTO;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.utils.PageUtils;
import com.smartLive.common.core.utils.StringUtils;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.search.api.RemoteSearchService;
import com.smartLive.shop.domain.ShopType;
import com.smartLive.shop.service.IShopTypeService;
import com.smartLive.shop.until.CacheClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import com.smartLive.shop.mapper.ShopMapper;
import com.smartLive.shop.domain.Shop;
import com.smartLive.shop.service.IShopService;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

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
    private RemoteSearchService remoteSearchService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

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
        flashShopListRedisCache(shop.getTypeId());
        return shopMapper.insertShop(shop);
    }

    /**
     * 修改店铺
     *
     * @param shop 店铺
     * @return 结果
     */
    @Override
    public int updateShop(Shop shop) {
        shop.setUpdateTime(DateUtils.getNowDate());
        flashShopRedisCache(shop.getId());
        return shopMapper.updateShop(shop);
    }

    /**
     * 批量删除店铺
     *
     * @param ids 需要删除的店铺主键
     * @return 结果
     */
    @Override
    public int deleteShopByIds(String[] ids) {
//        int i = shopMapper.deleteShopByIds(ids);
//        //删除es数据
//        if (i > 0) {
            for (String id : ids) {
                log.info("删除es数据：{}", id);
                EsInsertRequest esInsertRequest = new EsInsertRequest();
                esInsertRequest.setId(Long.valueOf(id));
                esInsertRequest.setIndexName(EsIndexNameConstants.SHOP_INDEX_NAME);
                esInsertRequest.setDataType(EsDataTypeConstants.SHOP);
                //发起rabbitMq信息删除
                rabbitTemplate.convertAndSend(MqConstants.ES_EXCHANGE,MqConstants.ES_ROUTING_SHOP_DELETE,esInsertRequest);
            }
//        }
        return 1;
    }

    /**
     * 删除店铺信息
     *
     * @param id 店铺主键
     * @return 结果
     */
    @Override
    public int deleteShopById(String id) {
        return shopMapper.deleteShopById(id);
    }


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    /**
     * 根据id查询商铺信息
     *
     * @param id 商铺id
     * @return 商铺详情数据
     */
    @Override
    public Result queryById(Long id) {
        //解决缓存穿透
//        Shop shop = queryWithPassThrough(id);
        //互斥锁解决缓存击穿
//        Shop shop = queryWithMutex(id);
        //逻辑过期来解决缓存击穿
//        Shop shop = queryWithLogicalExpire(id);
        //缓存穿透,使用工具类CacheClient
        Shop shop = cacheClient.queryWithPassThrough(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //逻辑过期解决缓存击穿 使用工具类CacheClient
//        Shop shop = cacheClient.queryWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY, id, Shop.class,this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    /**
     * 更新商铺数据
     *
     * @param shop 商铺数据
     * @return 商铺id
     */
    @Override
    @Transactional
    public Result updateShopById(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        // 写入数据库
        updateById(shop);
        // 删除缓存
        flashShopRedisCache(id);
        return null;
    }


    /**
     * 缓存穿透
     *
     * @param id
     * @return
     */
    public Shop queryWithPassThrough(Long id) {

        //从缓存里获取商铺数据
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            //存在，直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        //判断命中的是否是空值
        if (shopJson != null) {
            return null;
        }
        Shop shop = this.getById(id);
        if (shop == null) {
            //防止缓存穿透,将空值存入redis
            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //存入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return shop;

    }

    //创建缓存线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 逻辑过期来解决缓存穿透
     *
     * @param id
     * @return
     */
    public Shop queryWithLogicalExpire(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        //从缓存里获取商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
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
     * 互斥锁解决缓存穿透
     *
     * @param id
     * @return
     */
    public Shop queryWithMutex(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        //从缓存里获取商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
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
                stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            //存入redis
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
        } finally {
            unLock(lockKey);
        }
        //释放互斥锁
        unLock(lockKey);
        return shop;

    }

    /**
     * 获取锁
     *
     * @param key
     * @return
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放锁
     *
     * @param key
     */
    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 保存热点店铺数据到redis
     *
     * @param id
     * @param expireSeconds
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
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 根据类型分页查询商铺信息
     *
     * @param typeId  商铺类型
     * @param current 页码
     * @param x
     * @param y
     * @return 商铺列表
     */
    @Override
    public Result queryShopByType(Integer typeId, Integer current,String sortBy, Double x, Double y) {
        //判断是否根据坐标查询
        if (x == null && y == null) {
            String key = RedisConstants.SHOP_GEO_KEY + typeId;
            String shopJson = stringRedisTemplate.opsForList().leftPop(key);
            if(shopJson != null){
                List<Shop> shops = JSONUtil.toList(shopJson, Shop.class);
                return Result.ok(shops);
            }
            //不需要坐标查询，直接从数据库查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            //存入redis
            stringRedisTemplate.opsForList().leftPush(key, JSONUtil.toJsonStr( page.getRecords()));
            //返回数据
            return Result.ok(page.getRecords());
        }
        //计算分页参数
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;
        //查询redis、按照距离排序、分页查询 结果：shopId、distance
        String key = RedisConstants.SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(
                        key,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(200000),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
                );
        // 解析出id
        if (results == null) {
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if (list.size() <= from) {
            //没有下一页了，结束
            return Result.ok(Collections.emptyList());
        }
        //截取从from 到 end的部分
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        list.stream().skip(from).forEach(result -> {
            //获取店铺id
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            //获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr, distance);
        });
        //根据id查询shop
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops;
        if(!sortBy.equals("distance")){
            shops = query().in("id", ids).last("ORDER BY " + sortBy).list();
        }else {
            shops = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        }
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        return Result.ok(shops);
    }

    @Override
    public List<Shop> searchShopsByShopQuery(Shop shopQuery) {
        log.info("查询店铺信息的条件是：{}", shopQuery);
        List<Shop> list = query().list();
                        return list;
    }

    @Override
    public Shop selectShopByShop(Shop shopVO) {
        log.info("查询店铺详细信息的条件是：{}", shopVO);
        Shop shop = query()
                .eq(shopVO.getId() != null, "id", shopVO.getId())
                .like(shopVO.getName() != null, "name", shopVO.getName())
                .one();
        return shop;
    }

    /**
     * 根据商铺名称查询商铺信息
     *
     * @param shopName 商铺名称
     * @return 商铺详情
     */
    @Override
    public R<Shop> getShopByShopName(String shopName) {
        Shop shop = query().eq("name", shopName).one();
        return R.ok(shop);
    }

    /**
     * 修改商铺评论数量
     *
     * @param shopId 商铺id
     * @return 修改结果
     */
    @Override
    public R<Boolean> updateCommentById(Long shopId) {
        //更新评论数量
        boolean update = update().setSql("comments = comments + 1").eq("id", shopId).update();
        flashShopRedisCache(shopId);
        return R.ok(update);
    }

    /**
     * 根据条件查询商铺信息
     *
     * @param shop 搜索条件
     * @return 搜索结果
     */
    @Override
    public List<Shop> getShopByCondition(Shop shop) {
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
        return list(wrapper);
    }

    /**
     * 根据商铺id查询商铺信息
     *
     * @param shopId 商铺id
     * @return 商铺信息
     */
    @Override
    public R<Shop> getShopById(Long shopId) {
        Shop shop = query().eq("id", shopId).one();
        return R.ok(shop);
    }

    /**
     * 根据商铺id列表查询商铺信息列表
     *
     * @param ids 商铺id列表
     * @return 商铺列表
     */
    @Override
    public List<Shop> getShopList(List<Long> ids) {

        //根据用户id查询用户  where id in (5,2) order by field (id,5,2)
        String idStr = StrUtil.join(",",ids);
        List<Shop> orderList = query().in("id", ids).last("order by field(id," + idStr + ")").list();
//        userList = userList.stream().map(user -> {
//            UserInfo userInfo = userInfoService.getByUserId(user.getId());
//            if(userInfo != null){
//                user.setIntroduce(userInfo.getIntroduce());
//            }
//            return user;
//        }).collect(Collectors.toList());
        return orderList;
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
        stringRedisTemplate.delete(key);
        List<ShopType> shopTypeList = shopTypeService.list();
        shopTypeList.forEach(shopType -> {
            List<Shop> shopList = query().eq("type_id", shopType.getId()).list();
            //缓存
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_lIST_KEY+shopType.getId(), JSONUtil.toJsonStr(shopList));
        });


        //缓存店铺坐标数据
        List<Shop> list = list();
        //删除所有店铺的坐标缓存
        stringRedisTemplate.delete(stringRedisTemplate.keys(RedisConstants.SHOP_GEO_KEY+"*"));
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
                stringRedisTemplate.opsForGeo().add(shopGeoKey, new Point(shop.getX(), shop.getY()), shop.getId().toString());
            }
        }
        return "刷新成功";
    }

    /**
     * 清空店铺列表缓存
     *
     * @param typeId
     */
    private void flashShopListRedisCache(Long typeId) {
        //清空缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_lIST_KEY+typeId);
    }

    /**
     * 清空店铺缓存
     *
     * @param id
     */
    private void flashShopRedisCache(Long id){
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY+id);
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
                shops.forEach(
                        shop -> {
                            shop.setLocation(shop.getY() + "," + shop.getX());
                        }
                );
                // 创建请求并发送
                EsBatchInsertRequest request = new EsBatchInsertRequest();
                request.setIndexName(EsIndexNameConstants.SHOP_INDEX_NAME);
                request.setData(shops);
                request.setDataType(EsDataTypeConstants.SHOP);
                rabbitTemplate.convertAndSend(
                        MqConstants.ES_EXCHANGE,
                        MqConstants.ES_ROUTING_SHOP_BATCH_INSERT,
                        request
                );
                log.info("发送第 {} 页，{} 条数据", page, shops.size());
                page++;
            }
            return "数据发布完成";
        }


    /**
     * 发布店铺
     *
     * @param
     * @return 发布结果
     */
    @Override
    public String publish(String[] ids) {
        for (String id : ids) {
            Shop shop = query().eq("id", id).one();
            EsInsertRequest esInsertRequest = new EsInsertRequest();
            esInsertRequest.setIndexName(EsIndexNameConstants.SHOP_INDEX_NAME);
            shop.setLocation(shop.getY() + "," + shop.getX());
            esInsertRequest.setData(shop);
            esInsertRequest.setId(shop.getId());
            esInsertRequest.setDataType(EsDataTypeConstants.SHOP);
            rabbitTemplate.convertAndSend(MqConstants.ES_EXCHANGE, MqConstants.ES_ROUTING_SHOP_INSERT, esInsertRequest);

        }
        return "发布成功";
    }
}