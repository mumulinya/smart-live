package com.smartLive.common.redis.service;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.domain.geo.Metrics;
import org.springframework.stereotype.Component;
import org.springframework.data.geo.Point;
/**
 * spring redis 工具类
 * 
 * @author smartLive
 **/
@SuppressWarnings(value = { "unchecked", "rawtypes" })
@Component
@Slf4j
public class RedisService
{
    @Autowired
    public RedisTemplate redisTemplate;

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key 缓存的键值
     * @param value 缓存的值
     */
    public <T> void setCacheObject(final String key, final T value)
    {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key 缓存的键值
     * @param value 缓存的值
     * @param timeout 时间
     * @param timeUnit 时间颗粒度
     */
    public <T> void setCacheObject(final String key, final T value, final Long timeout, final TimeUnit timeUnit)
    {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 设置有效时间
     *
     * @param key Redis键
     * @param timeout 超时时间
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout)
    {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置有效时间
     *
     * @param key Redis键
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit)
    {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取有效时间
     *
     * @param key Redis键
     * @return 有效时间
     */
    public long getExpire(final String key)
    {
        return redisTemplate.getExpire(key);
    }

    /**
     * 判断 key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public Boolean hasKey(String key)
    {
        return redisTemplate.hasKey(key);
    }

    /**
     * 获得缓存的基本对象。
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    public <T> T getCacheObject(final String key)
    {
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.get(key);
    }
    /**
     * 缓存数据的自增
     *
     * @param key 缓存键值
     * @return 自增后的值 (new value)
     */
    public Long increment(final String key)
    {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 缓存数据的自增 (自定义步长)
     *
     * @param key 缓存键值
     * @param delta 自增步长
     * @return 自增后的值
     */
    public Long increment(final String key, long delta)
    {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 缓存数据的自减
     *
     * @param key 缓存键值
     * @return 自减后的值 (new value)
     */
    public Long decrement(final String key)
    {
        return redisTemplate.opsForValue().decrement(key);
    }

    /**
     * 缓存数据的自减 (自定义步长)
     *
     * @param key 缓存键值
     * @param delta 自减步长
     * @return 自减后的值
     */
    public Long decrement(final String key, long delta)
    {
        return redisTemplate.opsForValue().decrement(key, delta);
    }
    /**
     * 删除单个对象
     *
     * @param key
     */
    public boolean deleteObject(final String key)
    {
        return redisTemplate.delete(key);
    }

    /**
     * 删除集合对象
     *
     * @param collection 多个对象
     * @return
     */
    public boolean deleteObject(final Collection collection)
    {
        return redisTemplate.delete(collection) > 0;
    }

    /**
     * 缓存List数据
     *
     * @param key 缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象
     */
    public <T> long setCacheList(final String key, final List<T> dataList)
    {
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        return count == null ? 0 : count;
    }

    /**
     * 获得缓存的list对象
     *
     * @param key 缓存的键值
     * @return 缓存键值对应的数据
     */
    public <T> List<T> getCacheList(final String key)
    {
        return redisTemplate.opsForList().range(key, 0, -1);
    }
    /**
     * 缓存 List 数据
     * 将一个对象（如 List<Blog>）作为一个整体元素放入 Redis List 头部
     * 对应 Redis 命令: LPUSH key value
     *
     * @param key  缓存键值
     * @param dataList 缓存的数据（通常是 List 对象，但也可以是任何对象）
     * @return 列表当前的长度
     */
    public <T> Long setCacheList(final String key, final T dataList)
    {
        // RedisTemplate 会根据配置的 Serializer 自动将 dataList 序列化为 JSON 字符串存入
        return redisTemplate.opsForList().leftPush(key, dataList);
    }
    /**
     * 移出并获取列表的第一个元素
     * 对应 Redis 命令: LPOP key
     *
     * @param key 缓存键
     * @return 列表的第一个元素
     */
    public <T> T leftPopCacheList(final String key)
    {
        return (T) redisTemplate.opsForList().leftPop(key);
    }
    /**
     * 缓存Set
     *
     * @param key 缓存键值
     * @param dataSet 缓存的数据
     * @return 缓存数据的对象
     */
    public <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet)
    {
        BoundSetOperations<String, T> setOperation = redisTemplate.boundSetOps(key);
        Iterator<T> it = dataSet.iterator();
        while (it.hasNext())
        {
            setOperation.add(it.next());
        }
        return setOperation;
    }

    /**
     * 获得缓存的set
     *
     * @param key
     * @return
     */
    public <T> Set<T> getCacheSet(final String key)
    {
        return redisTemplate.opsForSet().members(key);
    }
    /**
     * 向 Set 缓存中添加单个数据
     * 对应 Redis 命令: SADD key member
     *
     * @param key   缓存键值
     * @param value 缓存的数据
     * @return 成功添加的数量 (1表示新添加，0表示已存在)
     */
    public <T> Long addCacheSet(final String key, final T value)
    {
        return redisTemplate.opsForSet().add(key, value);
    }
    /**
     * 缓存ZSet (批量添加)
     * 注意：ZSet 需要分数(Score)来排序。
     * 这里假设传入的是 TypedTuple 类型，它包含 Value 和 Score。
     *
     * @param key     缓存键值
     * @param objects Set<ZSetOperations.TypedTuple<T>> 包含值和分数的集合
     * @return 添加成功的数量
     */
    public <T> Long setCacheZSet(final String key, final Set<ZSetOperations.TypedTuple<T>> objects) {
        return redisTemplate.opsForZSet().add(key, objects);
    }

    /**
     * 缓存ZSet (单个添加)
     *
     * @param key   缓存键值
     * @param value 缓存的值
     * @param score 分数（排序权重）
     * @return 是否添加成功
     */
    public <T> Boolean setCacheZSet(final String key, final T value, final double score) {
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     * 获取缓存的ZSet (按排名范围)
     * 对应 Redis 命令: ZRANGE key start end
     *
     * @param key   缓存键
     * @param start 起始索引 (0 是第一个)
     * @param end   结束索引 (-1 是最后一个)
     * @return 指定范围内的元素集合
     */
    public <T> Set<T> getCacheZSetRange(final String key, final long start, final long end) {
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    /**
     * 获取有序集合的范围数据（降序/从大到小）
     * 对应 Redis 命令: ZREVRANGE key start end
     *
     * @param key   缓存键
     * @param start 起始索引 (0 表示第一个)
     * @param end   结束索引 (-1 表示最后一个)
     * @return 成员集合 (LinkedHashSet，保证顺序)
     */
    public <T> Set<T> getCacheZSetReverseRange(String key, long start, long end) {
        // 使用 stringRedisTemplate 以保证能读取到之前存入的 ID 字符串
        Set<T> range = redisTemplate.opsForZSet().reverseRange(key, start, end);
        // 最佳实践：如果 Redis 返回 null，返回空集合，防止业务层 NPE
        return range != null ? range : Collections.emptySet();
    }
    /**
     * 获取 ZSet 数据 (倒序 + 按分数范围 + 分页 + 携带分数)
     * 对应 Redis 命令: ZREVRANGEBYSCORE key max min [WITHSCORES] LIMIT offset count
     *
     * @param key    缓存键值
     * @param min    最小分数 (通常是 0)
     * @param max    最大分数 (通常是 System.currentTimeMillis() 或 Double.MAX_VALUE)
     * @param offset 偏移量 (Skip)
     * @param count  每页数量 (Limit)
     * @return 包含值和分数的 Tuple 集合
     */
    public <T> Set<ZSetOperations.TypedTuple<T>> getCacheZSetReverseRangeByScore(
            final String key,
            final double min,
            final double max,
            final long offset,
            final long count)
    {
        return redisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, min, max, offset, count);
    }
    /**
     * 获取 ZSet 数据 (倒序 + 按排名范围 + 携带分数)
     * 对应 Redis 命令: ZREVRANGE key start stop WITHSCORES
     *
     * @param key   缓存键值
     * @param start 起始索引 (0 表示第一名)
     * @param end   结束索引 (例如 9 表示第十名)
     * @return 包含值(String)和分数的 Tuple 集合
     */
    public Set<ZSetOperations.TypedTuple<String>> getCacheZSetReverseRangeWithScores(
            final String key,
            final long start,
            final long end)
    {
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
    }
    /**
     * 获取有序集合(ZSet)中指定元素的分数
     *
     * @param key
     * @return
     */
    public Double getCacheZSetScore(final String key, final Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }
    /**
     * 获取 ZSet 集合的大小
     * 对应 Redis 命令: ZCARD key
     *
     * @param key 缓存键
     * @return 集合中元素的数量，如果 key 不存在返回 0
     */
    public Long getCacheZSetSize(final String key) {
        try {
            Long size = redisTemplate.opsForZSet().zCard(key);
            return size == null ? 0L : size;
        } catch (Exception e) {
            return 0L; // 发生异常返回0，保证业务不崩
        }
    }
    /**
     * 计算两个 ZSet 的交集并存储到新 Key
     * 对应 Redis 命令: ZINTERSTORE destKey 2 key1 key2
     * (默认情况下，结果集中元素的分数是原集合分数的 和)
     *
     * @param key1    第一个集合 Key
     * @param key2    第二个集合 Key
     * @param destKey 存储结果的目标 Key
     * @return 交集结果集的元素数量
     */
    public Long intersectAndStoreZSet(String key1, String key2, String destKey) {
        Long count = redisTemplate.opsForZSet().intersectAndStore(key1, key2, destKey);
        return count != null ? count : 0L;
    }
    /**
     * 给 ZSet 中的元素分数增加 (自增)
     * 对应 Redis 命令: ZINCRBY key increment member
     *
     * @param key   缓存键
     * @param value 元素 (Member)
     * @param delta 增加的分数 (可以为负数实现自减)
     * @return 增加后的新分数
     */
    public Double incrementCacheZSetScore(String key, String value, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, value, delta);
    }
    /**
     * 删除 ZSet 中的指定元素 (Member)
     * 对应 Redis 命令: ZREM key member
     *
     * @param key   Redis 键
     * @param value ZSet 中的元素 (Member)
     * @return 是否删除成功 (true 表示移除成功，false 表示元素不存在)
     */
    public boolean removeCacheZSetObject(final String key, final Object value)
    {
        Long count = redisTemplate.opsForZSet().remove(key, value);
        return count != null && count > 0;
    }
    /**
     * 按索引范围删除 ZSet 中的元素
     * 对应 Redis 命令: ZREMRANGEBYRANK key start stop
     * 常用于定长队列，例如：只保留最新的 N 条数据
     *
     * @param key   缓存键
     * @param start 起始索引
     * @param end   结束索引
     * @return 被删除的元素数量
     */
    public Long removeRangeCacheZSetObject(String key, long start, long end) {
        Long count = redisTemplate.opsForZSet().removeRange(key, start, end);
        return count != null ? count : 0L;
    }
    /**
     * 缓存Map
     *
     * @param key
     * @param dataMap
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap)
    {
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    /**
     * 获得缓存的Map
     *
     * @param key
     * @return
     */
    public <T> Map<String, T> getCacheMap(final String key)
    {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 往Hash中存入数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @param value 值
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value)
    {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * 获取Hash中的数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     */
    public <T> T getCacheMapValue(final String key, final String hKey)
    {
        HashOperations<String, String, T> opsForHash = redisTemplate.opsForHash();
        return opsForHash.get(key, hKey);
    }

    /**
     * 获取多个Hash中的数据
     *
     * @param key Redis键
     * @param hKeys Hash键集合
     * @return Hash对象集合
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys)
    {
        return redisTemplate.opsForHash().multiGet(key, hKeys);
    }

    /**
     * 删除Hash中的某条数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @return 是否成功
     */
    public boolean deleteCacheMapValue(final String key, final String hKey)
    {
        return redisTemplate.opsForHash().delete(key, hKey) > 0;
    }


    /**
     * 添加地理位置信息
     * 对应 Redis 命令: GEOADD key longitude latitude member
     *
     * @param key    缓存键
     * @param x      经度 (Longitude)
     * @param y      纬度 (Latitude)
     * @param member 成员 (通常是店铺ID，可以是 Long 或 String)
     * @return 添加成功的数量
     */
    public <T> Long addCacheGeoLocation(final String key, final double x, final double y, final T member)
    {
        // 封装 new Point(x, y) 逻辑，业务层只需传坐标值
        return redisTemplate.opsForGeo().add(key, new Point(x, y), member);
    }

    /**
     * 根据坐标和距离查询 GEO 数据
     * 对应 Redis 命令: GEOSEARCH ...
     *
     * @param key        缓存键
     * @param x          经度
     * @param y          纬度
     * @param distanceKm 距离 (单位：千米，或者你可以把单位也作为参数传进来)
     * @param limit      限制查询数量
     * @return GEO 结果集
     */
    public GeoResults<RedisGeoCommands.GeoLocation<Object>> getCacheGeoLocation(
            final String key,
            final double x,
            final double y,
            final double distanceKm,
            final long limit)
    {
        // 构造查询参数：指定坐标、半径、单位(这里写死为米或千米)、返回距离、限制条数
        RedisGeoCommands.GeoSearchCommandArgs args = RedisGeoCommands.GeoSearchCommandArgs
                .newGeoSearchArgs()
                .includeDistance() // 结果包含距离
                .limit(limit);     // 限制条数

        return redisTemplate.opsForGeo().search(
                key,
                GeoReference.fromCoordinate(new Point(x, y)), // 中心点
                new Distance(distanceKm, Metrics.METERS),     // 半径和单位(注意你的业务是200000米还是千米，这里要注意)
                args
        );
    }

    /**
     * 执行 Lua 脚本
     *
     * @param script Lua 脚本对象
     * @param keys   Redis 键列表 (KEYS)
     * @param args   参数列表 (ARGV)
     * @param <T>    返回类型
     * @return 脚本执行结果
     */
    public <T> T executeScript(RedisScript<T> script, List<String> keys, Object... args) {
        // 使用 stringRedisTemplate 执行，确保参数和结果都作为 String 处理 (或根据 Script 定义自动转换)
        return (T) redisTemplate.execute(script, keys, args);
    }
    /**
     * 获得缓存的基本对象列表
     *
     * @param pattern 字符串前缀
     * @return 对象列表
     */
    public Collection<String> keys(final String pattern)
    {
        return redisTemplate.keys(pattern);
    }
    /**
     * 缓存重命名
     *
     * @param oldKey
     * @param newKey
     */
    public void rename(String oldKey, String newKey) {
        redisTemplate.rename(oldKey, newKey);
    }
}
