package com.smartLive.shop.until;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.constant.RedisData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class CacheClient {

    @Resource
    private  StringRedisTemplate stringRedisTemplate;

    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        //设置逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        //写入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData), time, unit);
    }


    /**
     * 缓存穿透
     * @param id
     * @return
     */
    public <R,ID>  R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {

        //从缓存里获取商铺数据
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if(StrUtil.isNotBlank(json)){
            //存在，直接返回
            return JSONUtil.toBean(json, type);
        }
        //判断命中的是否是空值
        if (json != null){
            //空值，直接返回
            return null;
        }
        //不存在，根据id查询数据库
        R r = dbFallback.apply(id);
        if(r == null){
            //防止缓存穿透,将空值存入redis
            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //存入redis
        this.set(key, r, time, unit);
        return r;

    }

    //创建缓存线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 逻辑过期来解决缓存击穿
     * @param id
     * @return
     */
    public <R,ID> R queryWithLogicalExpire(String keyPrefix, ID id,Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit)  {
        String key = keyPrefix + id;
        //从缓存里获取商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if(StrUtil.isBlank(json)){
            //存在，直接返回空
            return null;
        }
        //把json转换成对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        //判断是否过期
        if(redisData.getExpireTime().isAfter(LocalDateTime.now())){
            //未过期，直接返回数据
            return r;
        }
        //TODO 过期，缓存重建
        //获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        if (isLock){
            //取锁成功，开启独立线程,进行缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    //查询数据库
                    R r1 = dbFallback.apply(id);
                    //写入redis
                    this.setWithLogicalExpire(key, r1,time, unit);
                }catch (Exception e){

                }finally {
                    //释放锁
                    unLock(lockKey);
                }
            });
        }
        //返回过期的数据
        return r;

    }


    /**
     * 逻辑过期来解决缓存击穿+缓存穿透
     * @param id
     * @return
     */
    public <R,ID> R queryWithLogicalExpireAndPassThrough(String keyPrefix, ID id,Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit)  {
        String key = keyPrefix + id;
        //从缓存里获取商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        R r=null;
        //判断是否存在
        if(StrUtil.isNotBlank(json)){
            //把json转换成对象
            RedisData redisData = JSONUtil.toBean(json, RedisData.class);
             r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
            //判断是否过期
            if(redisData.getExpireTime().isAfter(LocalDateTime.now())){
                //未过期，直接返回数据
                return r;
            }
        }
        //判断命中的是否是空值
        if (json != null){
            //空值，直接返回
            return null;
        }
        //TODO 过期，缓存重建
        //获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        if (isLock){
            //取锁成功，开启独立线程,进行缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    //查询数据库
                    R r1 = dbFallback.apply(id);
                    if(r1 == null){
                        //防止缓存穿透,将空值存入redis
                        stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                    }else{
                        //写入redis
                        this.setWithLogicalExpire(key, r1,time, unit);
                    }
                }catch (Exception e){
                    log.error(e.getMessage());
                }finally {
                    //释放锁
                    unLock(lockKey);                    }
            });
        }
        //返回过期的数据
        return r;

    }

    /**
     * 获取redis锁
     * @param key
     * @return
     */
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放redis锁
     * @param key
     */
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }
}
