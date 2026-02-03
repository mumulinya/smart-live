package com.smartLive.blog.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.blog.domain.Blog;
import com.smartLive.blog.mapper.BlogMapper;
import com.smartLive.blog.service.IBlogService;
import com.smartLive.common.core.constant.*;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.enums.FeedTypeEnum;
import com.smartLive.common.rabbitmq.domain.FeedEventMessage;
import com.smartLive.common.rabbitmq.domain.ContentSyncMessage;
import com.smartLive.common.rabbitmq.domain.ContentBatchSyncMessage;
import com.smartLive.common.core.domain.ScrollResult;
import com.smartLive.common.core.domain.UserDTO;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.rabbitmq.domain.UserResourceMessage;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.interaction.api.RemoteLikeService;
import com.smartLive.interaction.api.RemoteStarService;
import com.smartLive.interaction.api.DTO.LikeDTO;
import com.smartLive.interaction.api.DTO.StarDTO;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.DTO.ShopDTO;
import com.smartLive.user.api.RemoteAppUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 博客Service业务层处理
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Service
@Slf4j
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService
{
    @Autowired
    private BlogMapper blogMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisService redisService;

    @Autowired
    private ExecutorService executorService;

    @Autowired
    private RemoteAppUserService remoteAppUserService;
    @Autowired
    private RemoteShopService remoteShopService;
    @Autowired
    private RemoteLikeService remoteLikeService;
    @Autowired
    private RemoteStarService remoteStarService;

    /**
     * 查询博客
     * 
     * @param id 博客主键
     * @return 博客
     */
    @Override
    public Blog selectBlogById(Long id)
    {
        return blogMapper.selectBlogById(id);
    }

    /**
     * 查询博客列表
     * 
     * @param blog 博客
     * @return 博客
     */
    @Override
    public List<Blog> selectBlogList(Blog blog)
    {
        return blogMapper.selectBlogList(blog);
    }

    /**
     * 新增博客
     * 
     * @param blog 博客
     * @return 结果
     */
    @Override
    public int insertBlog(Blog blog)
    {
        blog.setCreateTime(DateUtils.getNowDate());
        int i = blogMapper.insertBlog(blog);
        if(i > 0){
            //添加es数据
            publish(new String[]{blog.getId().toString()});
            //更新redis缓存
            redisService.deleteObject(RedisConstants.CACHE_HOT_BLOG_KEY+blog.getTypeId());
        }
        return i;
    }

    /**
     * 修改博客
     * 
     * @param blog 博客
     * @return 结果
     */
    @Override
    public int updateBlog(Blog blog)
    {
        blog.setUpdateTime(DateUtils.getNowDate());
        int i = blogMapper.updateBlog(blog);
        if(i > 0){
            //更新es数据
            publish(new String[]{blog.getId().toString()});
            flashRedisBlogCache(blog.getId());
        }
        return i;
    }

    /**
     * 批量删除博客
     * 
     * @param ids 需要删除的博客主键
     * @return 结果
     */
    @Override
    public int deleteBlogByIds(Long[] ids)
    {
        int i = blogMapper.deleteBlogByIds(ids);
        //删除es数据
        if (i > 0) {
        CountDownLatch latch=new CountDownLatch(ids.length);
        for (Long id : ids) {
            executorService.submit(()->{
               log.info("删除es数据：{}", id);
               ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
               contentSyncMessage.setId(id);
               contentSyncMessage.setIndexName(EsIndexNameConstants.BLOG_INDEX_NAME);
               contentSyncMessage.setType(GlobalBizTypeEnum.BLOG.getCode());
                //发起rabbitMq信息删除
//               rabbitTemplate.convertAndSend(MqConstants.ES_EXCHANGE,MqConstants.ES_ROUTING_BLOG_DELETE,esInsertRequest);
               MqMessageSendUtils.sendMqMessage(rabbitTemplate,MqConstants.ES_EXCHANGE,MqConstants.ES_ROUTING_BLOG_DELETE, contentSyncMessage);
               //更新redis缓存
               flashRedisBlogCache(id);
               latch.countDown();
           });
        }
        try {
            //等等所有任务完成
            log.info("等待所有任务完成");
            latch.await();
            log.info("所有任务完成");
            flashRedisBlogListCache();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        }
        return 1;
    }

    /**
     * 删除博客信息
     * 
     * @param id 博客主键
     * @return 结果
     */
    @Override
    public int deleteBlogById(Long id)
    {
        int i = blogMapper.deleteBlogById(id);
        if(i > 0){
            ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
            contentSyncMessage.setId(id);
            contentSyncMessage.setIndexName(EsIndexNameConstants.BLOG_INDEX_NAME);
            contentSyncMessage.setType(GlobalBizTypeEnum.BLOG.getCode());
            //发起rabbitMq信息删除
            MqMessageSendUtils.sendMqMessage(rabbitTemplate,MqConstants.ES_EXCHANGE,MqConstants.ES_ROUTING_BLOG_DELETE, contentSyncMessage);
            //更新redis缓存
            flashRedisBlogCache(id);
        }
        return i;
    }




    /**
     * 查询博客id查询博文详情
     *
     * @param id
     * @return
     */
    @Override
    public Blog queryBlogById(Long id) {
        //从redis查询博客缓存
        String key= RedisConstants.CACHE_BLOG_KEY+id;
        String blogJson =redisService.getCacheObject(key);
        if (blogJson != null) {
            //存在
            Blog blog = JSONUtil.toBean(blogJson, Blog.class);
            isBlogLiked(blog);
            //判断当前用户是否已经收藏
            isBlogStared(blog);
            return blog;
        }
        //根据博客id查询博客信息
        Blog blog = getById(id);
        if (blog == null) {
            throw new BusinessException("数据不存在");
        }
        // 查询blog有关的用户信息
        queryBlogUser(blog);
        //把博客信息存入redis
        redisService.setCacheObject(key, JSONUtil.toJsonStr(blog));
        //查询blog是否被点赞
        isBlogLiked(blog);
        //返回结果
        return blog;
    }

    /**
     * 判断当前用户是否已经点赞
     * @param blog
     */
    private void isBlogLiked(Blog blog) {
        UserDTO user = UserContextHolder.getUser();
        if (user == null) {
            //未登录,不用查询是否点赞
            blog.setIsLike(false);
            return;
        }
        //获取当前登录用户
        Long userId = user.getId();
        LikeDTO likeDTO = new LikeDTO();
        likeDTO.setUserId(userId);
        likeDTO.setSourceId(blog.getId());
        likeDTO.setSourceType(GlobalBizTypeEnum.BLOG.getCode());
        //判断当前用户是否已经点赞
        Boolean isLike = remoteLikeService.isLike(likeDTO);
        blog.setIsLike(isLike);
    }

    /**
     * 判断当前用户是否已经收藏博客
     * @param blog
     */
    private void isBlogStared(Blog blog) {
        UserDTO user = UserContextHolder.getUser();
        if (user == null) {
            //未登录,不用查询是否点赞
            blog.setIsStared(false);
            return;
        }
        //获取当前登录用户
        Long userId = user.getId();
        StarDTO starDTO = new StarDTO();
        starDTO.setUserId(userId);
        starDTO.setSourceId(blog.getId());
        starDTO.setSourceType(GlobalBizTypeEnum.BLOG.getCode());
        //判断当前用户是否已经收藏
        Boolean isStared = remoteStarService.isStar(starDTO);
        blog.setIsStared(isStared);
    }

    /**
     * 查询最热博客
     *
     * @param current
     * @return
     */
    @Override

    public List<Blog> queryHotBlog(Integer current) {
        //从redis查询热门博客
        String key= RedisConstants.CACHE_HOT_BLOG_KEY+ current;
        List<Blog> blogList = getBlogListFromRedis(key);
        if (blogList != null) {
            blogList.forEach(blog ->{
                isBlogLiked(blog);
            });
            return blogList;
        }
        // 根据用户查询
        Page<Blog> page = query()
                .eq("status",0)
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        // 获取当前页数据
         blogList = page.getRecords();
        if(blogList!= null&&blogList.size()>0){
            // 查询blog有关的用户信息
            blogList.forEach(blog ->{
                queryBlogUser(blog);
            });
            //把查询结果写入redis
            redisService.setCacheObject(key, JSONUtil.toJsonStr(blogList), RedisConstants.CACHE_HOT_BLOG_TTL, TimeUnit.DAYS);
        }
        return blogList;
    }

    /**
     * 查询blog有关的用户信息
     * @param blog
     */
    private void queryBlogUser(Blog blog){
        Long userId = blog.getUserId();
        //根据用户id获取用户信息
        com.smartLive.user.api.domain.UserDTO user= remoteAppUserService.queryUserById(userId);
        if (user != null) {
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
        }
    }

    /**
     * 点赞博客
     *
     * @param id
     * @return
     */
    @Override
    public Boolean likeBlog(Long id) {
        //获取当前登录用户
        UserDTO user = UserContextHolder.getUser();
        if (user == null) {
            //未登录
            throw new BusinessException("请登录");
        }
        Long userId = user.getId();
        //判断当前用户是否已经点赞
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        Double score = redisService.getCacheZSetScore(key, userId.toString());
        boolean isSuccess = false;
        if (score!=null) {
            //已经点赞了,取消点赞
            isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            if (isSuccess) {
                //删除用户点赞信息
                redisService.removeCacheZSetObject(key, userId.toString());
            }
        }else{
            //未点赞
            //修改点赞数量
            isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            //保存用户点赞信息到redis的set集合 zadd key value score
            if (isSuccess) {
                //保存用户点赞信息
                redisService.setCacheZSet(key, userId.toString(), System.currentTimeMillis());
            }
        }
        //清空缓存
        if (isSuccess) {
            //更新缓存
            flashRedisBlogCache(id);
            flashRedisBlogListCache();
        }
        return isSuccess;
    }

    /**
     * 查询用户发布的博客
     *
     * @param current
     * @param userId
     * @return
     */
    @Override
    public List<Blog> queryBlogByUserId(Integer current, Long userId) {
        Page<Blog> page = query()
                .eq("user_id", userId)
                .eq("status",0)
                .orderByDesc("pin")
                .orderByAsc("create_time")
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        List<Blog> records = page.getRecords();
        records.forEach(blog ->{
            isBlogLiked(blog);
        });
        return records;
    }

    /**
     * 保存博客
     *
     * @param blog
     * @return
     */
    @Override
    public Long saveBlog(Blog blog) {
        blog.setUserId(UserContextHolder.getUser().getId());
        blog.setCreateTime(DateUtils.getNowDate());
       if (blog.getShopId() != null) {
           ShopDTO shopDTO = remoteShopService.getShopById(blog.getShopId());
           if(shopDTO!= null){
               blog.setTypeId(shopDTO.getTypeId());
           }
        }
        // 保存探店笔记
        boolean success = save(blog);
        if (!success) {
            throw new BusinessException("新增博文失败");
        }
        //保存草稿
        if(blog.getStatus() == 1){
            return blog.getId();
        }
        //发送rabbitMq消息 推送笔记id给粉丝
        FeedEventMessage feedEventMessage = FeedEventMessage.builder()
                //事件类型
                .feedType(FeedTypeEnum.USER_FEED.getCode())
                //发送者类型
                .sourceType(GlobalBizTypeEnum.USER.getCode())
                .sourceId(blog.getUserId())
                //发送数据类型
                .bizType(GlobalBizTypeEnum.BLOG.getCode())
                .bizId(blog.getId())
                .publishTime(blog.getCreateTime())
                .build();
        MqMessageSendUtils.sendMqMessage(rabbitTemplate,MqConstants.INTERACT_FEED_EXCHANGE_NAME, MqConstants.INTERACT_FEED_BLOG_ROUTING, feedEventMessage);
        //添加es数据
        publish(new String[]{blog.getId().toString()});
        //添加用户es数据
        String actionType=UserResourceActionTypeConstants.USER_RESOURCE_ACTION_PUBLISH;
        queryBlogUser(blog);
        String  id = blog.getUserId()+"_"+actionType+"_"+GlobalBizTypeEnum.BLOG.getBizDomain()+"_"+blog.getId().toString();        UserResourceMessage userResourceMessage = UserResourceMessage.builder()
                .indexName(EsIndexNameConstants.USER_RESOURCE_INDEX_NAME)
                .id(id)
                .userId(blog.getUserId())
                .sourceId(blog.getId())
                .sourceType(GlobalBizTypeEnum.BLOG.getCode())
                .actionType(actionType)
                .data(blog)
                .build();
        MqMessageSendUtils.sendMqMessage(rabbitTemplate, MqConstants.ES_EXCHANGE,MqConstants.ES_ROUTING_USER_RESOURCE_INSERT, userResourceMessage);
        //更新redis缓存
        redisService.deleteObject(RedisConstants.CACHE_HOT_BLOG_KEY+blog.getTypeId());
        //返回id
        return blog.getId();
    }

    /**
     * 查询用户关注的用户发布的博客
     *
     * @param max
     * @param offset
     * @return
     */
    @Override
    public ScrollResult queryBlogByFollow(Long max, Integer offset) {
        if(UserContextHolder.getUser() == null){
            return new ScrollResult();
        }
        //获取当前登录用户
        Long userId = UserContextHolder.getUser().getId();
        String key = RedisConstants.USER_FEED_KEY+GlobalBizTypeEnum.BLOG.getBizDomain()+":" + userId;
        //查询收件箱 关注的用户发布的博客
        Set<ZSetOperations.TypedTuple<Object>> typedTuples = redisService.getCacheZSetReverseRangeByScore(key, 0,max , offset, 2);
        if (typedTuples == null || typedTuples.isEmpty()) {
            return new ScrollResult();
        }

        //获取博客id解析数据：blogId  minTime(时间戳) offset
        List<Long> blogIdList = new ArrayList<>(typedTuples.size());
        long minTime = 0L;
        int os = 1;
        for (ZSetOperations.TypedTuple<Object> typedTuple : typedTuples) {
            //获取博客id
            String idStr = String.valueOf(typedTuple.getValue());
            blogIdList.add(Long.valueOf(idStr));
            Long time = typedTuple.getScore().longValue();
            if (minTime == time) {
                os++;
            }else {
                minTime = time;
                os = 1;
            }
        }

        //根据id查询博客
        String idsStr = StrUtil.join(",",blogIdList);
        List<Blog> blogList = query().in("id",blogIdList).last("order by field(id,"+idsStr+")").list();
        blogList.forEach(blog ->{
            //查询blog有关的用户信息
            queryBlogUser(blog);
            //查询blog是否被点赞
            isBlogLiked(blog);
        });
        //封装响应数据
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogList);
        scrollResult.setOffset(os);
        scrollResult.setMinTime(minTime);
        return scrollResult;
    }

    /**
     * 更新博客的评论数
     *
     * @param blogId
     * @return
     */
    @Override
    public Boolean updateCommentById(Long blogId) {
        boolean update = update().setSql("comments = comments + 1").eq("id", blogId).update();
        if (update) {
            //更新缓存
            flashRedisBlogCache(blogId);
        }
        return update;
    }

    /**
     * 查询我的博客
     *
     * @param current
     * @return
     */
    @Override
    public List<Blog> queryMyBlog(Blog b,Integer current) {
        UserDTO user = UserContextHolder.getUser();
        // 根据用户查询
        Page<Blog> page = query()
                .eq("user_id", user.getId())
                .eq("status",b.getStatus())
                .orderByDesc("pin")
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> blogList = page.getRecords();
        blogList.forEach(blog ->{
            //查询blog有关的用户信息
            queryBlogUser(blog);
            //查询blog是否被点赞
            isBlogLiked(blog);
        });
        return blogList;
    }

    /**
     * 查询博客详情
     *
     * @param id
     * @return
     */
    @Override
    public Blog getBlogById(Long id) {
        Blog blog = query().eq("id", id).one();
        queryBlogUser(blog);
        return blog;
    }

    /**
     * 查询用户博客数量
     *
     * @param userId
     * @return
     */
    @Override
    public Integer getBlogCount(Long userId) {
        //查询数量
        Long count = query().eq("user_id", userId).count();

        Integer blogCount = count.intValue(); // 直接转换，超出范围会截断
        return blogCount;
    }

    /**
     * 查询用户博客点赞数量
     *
     * @param userId
     * @return
     */
    @Override
    public Integer getLikeCount(Long userId) {
        List<Blog> blogList = query().eq("user_id", userId).list();
        Integer likeCount = blogList.stream().mapToInt(blog -> blog.getLiked()).sum();
        return likeCount;
    }

    /**
     * 获取博客总数
     *
     * @return
     */
    @Override
    public Integer getBlogTotal() {
        return query().count().intValue();
    }

    /**
     * 获取博客列表
     *
     * @param sourceIdList
     * @return
     */
    @Override
    public List<Blog> getBlogListByIds(List<Long> sourceIdList) {
        String idStr = StrUtil.join(",", sourceIdList);
        List<Blog> blogList = query().in("id", sourceIdList).last("order by field(id," + idStr + ")").list();
        blogList.forEach(blog ->{
            //查询blog有关的用户信息
            queryBlogUser(blog);
            //查询blog是否被点赞
            isBlogLiked(blog);
        });
        return blogList;
    }

    /**
     * 置顶博客
     *
     * @param blog
     * @return 结果
     */
    @Override
    public boolean isPin(Blog blog) {
        boolean update = this.lambdaUpdate()
                .eq(Blog::getId, blog.getId())
                .set(Blog::getPin, blog.getPin())
                .update();
        if (update) {
            //更新缓存
            flashRedisBlogCache(blog.getId());
        }
        return update;
    }

    /**
     * 批量更新点赞数
     *
     * @param updateMap
     * @return
     */
    @Override
    public Boolean updateLikeCountBatch(Map<Long, Integer> updateMap) {
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
                baseMapper.updateLikeCountBatch(batchMap);
            }
        } else {
            // 数量少直接执行
            baseMapper.updateLikeCountBatch(updateMap);
        }
        flashCache();
        return true;
    }

    /**
     * 批量更新评论数
     *
     * @param updateMap
     * @return
     */
    @Override
    public Boolean updateCommentCountBatch(Map<Long, Integer> updateMap) {
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
                baseMapper.updateCommentCountBatch(batchMap);
            }
        } else {
            // 数量少直接执行
            baseMapper.updateCommentCountBatch(updateMap);
        }
        flashCache();
        return true;
    }

    /**
     * 批量更新收藏数
     *
     * @param updateMap
     * @return
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
        flashCache();
        return true;
    }

    /**
     * 获取博客点赞数
     *
     * @param sourceId
     * @return
     */
    @Override
    public Integer getBlogLikeCount(Long sourceId) {
        Blog blog = query().eq("id", sourceId).one();
        return blog.getLiked();
    }

    /**
     * 获取博客收藏数
     *
     * @param sourceId
     * @return
     */
    @Override
    public Integer getBlogStarCount(Long sourceId) {
        Blog blog = query().eq("id", sourceId).one();
        return blog.getStared();
    }

    /**
     * 刷新缓存
     *
     * @return
     */
    @Override
    public String flashCache() {
        redisService.deleteObject(redisService.keys(RedisConstants.CACHE_HOT_BLOG_KEY+"*"));
        redisService.deleteObject(redisService.keys(RedisConstants.CACHE_BLOG_KEY+"*"));
        redisService.deleteObject(redisService.keys(RedisConstants.CACHE_BLOG_TYPE_KEY+"*"));
        return null;
    }

    /**
     * 查询分类下的博客
     *
     * @param typeId
     * @param current
     * @return
     */
    @Override
    public List<Blog> queryBlogByCategory(Long typeId, Integer current) {
        //从redis查询分类博客
        String key= RedisConstants.CACHE_BLOG_TYPE_KEY + typeId+":"+ current;
        List<Blog> blogList = getBlogListFromRedis(key);
        if (blogList != null) {
                blogList.forEach(blog -> isBlogLiked(blog));
            return blogList;
        }
        Page<Blog> page = query()
                .select("images","liked","user_id","title","id")
                .eq("type_id", typeId)
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        blogList = page.getRecords();
        if(blogList!= null&&blogList.size()>0){
            // 查询blog有关的用户信息
            blogList.forEach(blog ->{
                queryBlogUser(blog);
                isBlogLiked(blog);
            });
            //把查询结果写入redis
            redisService.setCacheObject(key, JSONUtil.toJsonStr(blogList), RedisConstants.CACHE_HOT_BLOG_TTL, TimeUnit.DAYS);
        }
        return blogList;
    }
    /**
     * 从redis中获取博客列表
     *
     * @param key
     * @return
     */
    private List<Blog> getBlogListFromRedis(String key) {
        Object blogJson = redisService.getCacheObject(key);
        if (blogJson != null) {
            //存在
            List<Blog> blogs = JSONUtil.toList(blogJson.toString(), Blog.class);
            //获取用户是否点赞
            blogs.forEach(blog ->{
                isBlogLiked(blog);
            });
            return blogs;
        }
        return null;
    }
    /**
     * 全部发布博客
     *
     * @return 全部发布结果
     */
    @Override
    public String allPublish() {
        int page = PageConstants.PAGE_NUMBER;
        int pageSize =5; // 每页50条
        while (true) {
            // 分页查询
            List<Blog> blogs = query()
                    .page(new Page<>(page, pageSize))
                    .getRecords();
            if (blogs.isEmpty()) {
                break;
            }
            //使用多线程批量插入
            int finalPage = page;
            executorService.execute(() -> {
                //查询userId列表
                List<Long> userIds = blogs.stream()
                        .map(Blog::getUserId)
                        .filter(Objects::nonNull) // 防止有 null 的 userId 导致报错
                        .distinct()               // 去重，避免重复查询同一个 ID
                        .collect(Collectors.toList());

                // 定义结果 Map，默认为空
                Map<Long, com.smartLive.user.api.domain.UserDTO> userMap = Collections.emptyMap();

                // 2. 只有当 ID 列表不为空时才发起远程调用，节省资源
                if (!userIds.isEmpty()) {
                    // 批量查询用户信息
                    List<com.smartLive.user.api.domain.UserDTO> userList = remoteAppUserService.getUserList(userIds);
                    // 4. 将 List<User> 转换为 Map<Long, User>
                    if (userList != null) {
                        userMap = userList.stream().collect(Collectors.toMap(
                                com.smartLive.user.api.domain.UserDTO::getId,               // Key: 用户 ID
                                Function.identity(),       // Value: User 对象本身
                                (v1, v2) -> v1             // MergeFunction: 如果远程服务返回了重复 ID 的数据，取第一个，防止报错
                        ));
                    }
                }
                Map<Long, com.smartLive.user.api.domain.UserDTO> finalUserMap = userMap;
                blogs.forEach(blog ->{
                    //查询blog有关的用户信息
                    com.smartLive.user.api.domain.UserDTO user = finalUserMap.get(blog.getUserId());
                    blog.setName(user.getNickName());
                    blog.setIcon(user.getIcon());
                });
                // 创建请求并发送
                ContentBatchSyncMessage request = new ContentBatchSyncMessage();
                request.setIndexName(EsIndexNameConstants.BLOG_INDEX_NAME);
                request.setData(blogs);
                request.setType(GlobalBizTypeEnum.BLOG.getCode());
//                rabbitTemplate.convertAndSend(
//                        MqConstants.ES_EXCHANGE,
//                        MqConstants.ES_ROUTING_BLOG_BATCH_INSERT,
//                        request
//                );
                MqMessageSendUtils.sendMqMessage(rabbitTemplate,
                        MqConstants.ES_EXCHANGE,
                        MqConstants.ES_ROUTING_BLOG_BATCH_INSERT,
                        request);
                log.info("线程{}，发送第 {} 页，{} 条数据",Thread.currentThread().getName(),finalPage, blogs.size());
            });
//            blogs.forEach(blog ->{
//                //查询blog有关的用户信息
//                queryBlogUser(blog);
//            });
//            // 创建请求并发送
//            EsBatchInsertRequest request = new EsBatchInsertRequest();
//            request.setIndexName(EsIndexNameConstants.BLOG_INDEX_NAME);
//            request.setData(blogs);
//            request.setDataType(EsDataTypeConstants.BLOG);
//            rabbitTemplate.convertAndSend(
//                    MqConstants.ES_EXCHANGE,
//                    MqConstants.ES_ROUTING_BLOG_BATCH_INSERT,
//                    request
//            );
            page++;
        }
        return "数据发布完成";
    }


    /**
     * 发布博客
     *
     * @param
     * @return 发布结果
     */
    @Override
    public String publish(String[] ids) {
        for (String id : ids) {
          executorService.submit(() -> {
              log.info("线程{}，开始发布博客{}",Thread.currentThread().getName(),id);
              Blog blog = query().eq("id", id).one();
              if(blog == null){
                  return;
              }
              queryBlogUser(blog);
              ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
              contentSyncMessage.setIndexName(EsIndexNameConstants.BLOG_INDEX_NAME);
              contentSyncMessage.setData(blog);
              contentSyncMessage.setId(blog.getId());
              contentSyncMessage.setType(GlobalBizTypeEnum.BLOG.getCode());
//              rabbitTemplate.convertAndSend(MqConstants.ES_EXCHANGE, MqConstants.ES_ROUTING_BLOG_INSERT, esInsertRequest);
              MqMessageSendUtils.sendMqMessage(rabbitTemplate,
                      MqConstants.ES_EXCHANGE,
                      MqConstants.ES_ROUTING_BLOG_INSERT,
                      contentSyncMessage);
          });
        }
        return "发布成功";
    }

    /**
     * 清空当前博客缓存
     *
     * @param blogId
     */
    private void flashRedisBlogCache(Long blogId) {
        //清空缓存
        redisService.deleteObject(RedisConstants.CACHE_BLOG_KEY+blogId);
    }
    /**
     * 清空博客列表缓存
     *
     * @param
     */
    private void flashRedisBlogListCache() {
        redisService.deleteObject(redisService.keys(RedisConstants.CACHE_BLOG_TYPE_KEY+"*"));
        redisService.deleteObject(redisService.keys(RedisConstants.CACHE_HOT_BLOG_KEY+"*"));
    }
}
