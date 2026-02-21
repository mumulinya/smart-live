package com.smartLive.blog.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.blog.domain.Blog;
import com.smartLive.blog.domain.VO.BlogVO;
import com.smartLive.blog.mapper.BlogMapper;
import com.smartLive.blog.service.IBlogService;
import com.smartLive.common.rabbitmq.domain.*;
import org.springframework.beans.BeanUtils;
import com.smartLive.common.core.constant.*;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.enums.FeedTypeEnum;
import com.smartLive.common.core.domain.UserDTO;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.common.redis.util.CacheClient;
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
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.smartLive.common.redis.util.RedisMultiCacheManager;

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
    @Autowired
    private RedisMultiCacheManager redisMultiCacheManager;
    @Autowired
    private CacheClient cacheClient;

    /**
     * 将Blog实体转换为BlogVO
     * @param blog Blog实体
     * @return BlogVO对象
     */
    private BlogVO convertToBlogVO(Blog blog) {
        if (blog == null) {
            return null;
        }
        BlogVO blogVO = new BlogVO();
        BeanUtils.copyProperties(blog, blogVO);
        return blogVO;
    }

    /**
     * 将Blog列表转换为BlogVO列表
     * @param blogList Blog实体列表
     * @return BlogVO列表
     */
    private List<BlogVO> convertToBlogVOList(List<Blog> blogList) {
        if (CollUtil.isEmpty(blogList)) {
            return new ArrayList<>();
        }
        return blogList.stream()
                .map(this::convertToBlogVO)
                .collect(Collectors.toList());
    }

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
            blog=getById(blog.getId());
            queryBlogUser(blog);
            //发送审核消息
            sendAuditMessage(blog);
            flashRedisBlogCache(blog.getId());
            flashRedisBlogListCache();
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
               MqMessageSendUtils.sendMqMessage(rabbitTemplate,MqConstants.ES_EXCHANGE,MqConstants.ES_ROUTING_DELETE, contentSyncMessage);
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
            MqMessageSendUtils.sendMqMessage(rabbitTemplate,MqConstants.ES_EXCHANGE,MqConstants.ES_ROUTING_DELETE, contentSyncMessage);
            //更新redis缓存
            flashRedisBlogCache(id);
            flashRedisBlogListCache();
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
    public BlogVO queryBlogById(Long id) {
        Blog blog = cacheClient.queryWithLogicalExpireAndPassThrough(
                RedisConstants.CACHE_BLOG_KEY,
                id,
                Blog.class,
                this::getById,
                RedisConstants.CACHE_BLOG_TTL,
                TimeUnit.MINUTES
        );
        if (blog == null) {
            throw new BusinessException("数据不存在");
        }
        queryBlogUser(blog);
        isBlogLiked(blog);
        isBlogStared(blog);
        return convertToBlogVO(blog);
    }
    /**
     * 查询最热博客
     *
     * @param current
     * @return
     */
    @Override

    public List<BlogVO> queryHotBlog(Integer current) {
        //从redis查询热门博客
        String key= RedisConstants.CACHE_HOT_BLOG_KEY+ current;
        List<Blog> blogList = getBlogListFromRedis(key);
        if (blogList != null) {
            queryBlogListIsLike(blogList);
            return convertToBlogVOList(blogList);
        }
        // 根据用户查询
        Page<Blog> page = query()
                .eq("status",0)
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        // 获取当前页数据
         blogList = page.getRecords();
        if(blogList!= null&& !blogList.isEmpty()){
            // 查询blog有关的用户信息
            queryBlogListUserMessage(blogList);
            queryBlogListIsLike(blogList);
            //把查询结果写入redis
            redisService.setCacheObject(key, JSONUtil.toJsonStr(blogList), RedisConstants.CACHE_HOT_BLOG_TTL, TimeUnit.DAYS);
        }
        return convertToBlogVOList(blogList); // Changed from return blogList;
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
        MqMessageSendUtils.sendMqMessage(rabbitTemplate,MqConstants.INTERACT_FEED_EXCHANGE_NAME, MqConstants.INTERACT_FEED_ROUTING, feedEventMessage);
        //添加es数据
        publish(new String[]{blog.getId().toString()});
        //添加用户es数据
        String actionType=UserResourceActionTypeConstants.USER_RESOURCE_ACTION_PUBLISH;
        queryBlogUser(blog);
        //发送审核消息
        sendAuditMessage(blog);
        String  id = blog.getUserId()+"_"+actionType+"_"+GlobalBizTypeEnum.BLOG.getBizDomain()+"_"+blog.getId().toString();
        UserResourceMessage userResourceMessage = UserResourceMessage.builder()
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
     * 发送审核消息
     * @param blog
     */
    private void sendAuditMessage(Blog blog) {
        AuditMessage auditMessage = AuditMessage.builder()
                .bizId(blog.getId())
                .bizType(GlobalBizTypeEnum.BLOG.getCode())
                .submitterId(blog.getUserId())
                .auditContent(BeanUtil.beanToMap(blog))
                .createTime(blog.getCreateTime())
                .build();
        MqMessageSendUtils.sendMqMessage(rabbitTemplate, MqConstants.AUDIT_EXCHANGE_NAME,MqConstants.AUDIT_ROUTING_KEY, auditMessage);
    }

    /**
     * 查询我的博客
     *
     * @param current
     * @return
     */
    @Override
    public List<BlogVO> queryMyBlog(Blog b,Integer current) {
        UserDTO user = UserContextHolder.getUser();
        if (user == null) {
            throw new BusinessException("未登录");
        }
        // 根据用户查询
        Page<Blog> page = query()
                .eq("user_id", user.getId())
                .eq("status",b.getStatus())
                .orderByDesc("pin")
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> blogList = page.getRecords();
        queryBlogListUserMessage(blogList);
        queryBlogListIsLike(blogList);
        return convertToBlogVOList(blogList);
    }
    /**
     * 查询用户发布的博客
     *
     * @param current
     * @param userId
     * @return
     */
    @Override
    public List<BlogVO> queryBlogByUserId(Integer current, Long userId) {
        Page<Blog> page = query()
                .eq("user_id", userId)
                .eq("status",0)
                .orderByDesc("pin")
                .orderByAsc("create_time")
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        List<Blog> records = page.getRecords();
        queryBlogListIsLike(records);
        return convertToBlogVOList(records);
    }
    /**
     * 查询博客详情
     *
     * @param id
     * @return
     */
    @Override
    public BlogVO getBlogById(Long id) {
        Blog blog = query().eq("id", id).one();
        queryBlogUser(blog);
        return convertToBlogVO(blog);
    }

    /**
     * 获取博客列表
     *
     * @param sourceIdList
     * @return
     */
    @Override
    public List<Blog> getBlogListByIds(List<Long> sourceIdList) {
        // 1. Utilize RedisBatchCacheUtil for cached batch retrieval
        List<Blog> blogList = redisMultiCacheManager.queryBatchWithCache(
                RedisConstants.CACHE_BLOG_KEY,
                sourceIdList,
                Blog.class,
                missingIds -> {
                    // DB Fallback
                    String idStr = StrUtil.join(",", missingIds);
                    return query().in("id", missingIds)
                            .last("order by field(id," + idStr + ")")
                            .list();
                },
                Blog::getId,
                // Using general blog TTL or similar? Reusing PRODUCT/SHOP TTL logic or existing RedisConstants.CACHE_HOT_BLOG_TTL (which is 1 day). 
                // Let's use 30 minutes like others or defined constant. 
                // RedisConstants.CACHE_BLOG_TTL is not defined, but RedisConstants.CACHE_HOT_BLOG_TTL is. 
                // Let's use 30 minutes (generic TTL) or create one. 
                // For now, I'll use 30L and TimeUnit.MINUTES directly as per other services.
                30L,
                TimeUnit.MINUTES
        );

        // 2. Populate dynamic info (isLiked, isStared) in Batch
        if (CollUtil.isNotEmpty(blogList)) {
            queryBlogListUserMessage(blogList);
            queryBlogListIsLike(blogList);
        }
        return blogList;
    }

    /**
     * 批量查询博客是否被点赞
     * @param blogList
     */
    private void queryBlogListIsLike(List<Blog> blogList) {
        if (CollUtil.isEmpty(blogList)) {
            return;
        }
        UserDTO user = UserContextHolder.getUser();
        if (user == null) {
            blogList.forEach(blog -> {
                if (blog != null) {
                    blog.setIsLike(false);
                }
            });
            return;
        }
        // Extract IDs
        List<Long> blogIds = blogList.stream()
                .map(Blog::getId)
                .collect(Collectors.toList());

        // Batch check Likes
        LikeDTO likeDTO = new LikeDTO();
        likeDTO.setUserId(user.getId());
        likeDTO.setSourceType(GlobalBizTypeEnum.BLOG.getCode());
        Map<Long, Boolean> likeMap = remoteLikeService.getIsLikeBatch(likeDTO, blogIds);
        log.info("likeMap: {}", likeMap);
        blogList.forEach(blog -> {
            if (blog != null) {
                blog.setIsLike(likeMap.getOrDefault(blog.getId(), false));
            }
        });
    }

    /**
     * 批量查询博客是否被收藏
     * @param blogList
     */
    private void queryBlogListIsStar(List<Blog> blogList) {
        if (CollUtil.isEmpty(blogList)) {
            return;
        }
        UserDTO user = UserContextHolder.getUser();
        if (user == null) {
            blogList.forEach(blog -> {
                if (blog != null) {
                    blog.setIsStared(false);
                }
            });
            return;
        }
        // Extract IDs
        List<Long> blogIds = blogList.stream()
                .map(Blog::getId)
                .collect(Collectors.toList());

        // Batch check Stars
        StarDTO starDTO = new StarDTO();
        starDTO.setUserId(user.getId());
        starDTO.setSourceType(GlobalBizTypeEnum.BLOG.getCode());
        Map<Long, Boolean> starMap = remoteStarService.getIsStarBatch(starDTO, blogIds);

        blogList.forEach(blog -> {
            if (blog != null) {
                blog.setIsStared(starMap.getOrDefault(blog.getId(), false));
            }
        });
    }

    /**
     * 批量查询博客有关的用户信息
     * @param blogList
     */
    private void queryBlogListUserMessage(List<Blog> blogList) {
        if (CollUtil.isEmpty(blogList)) {
            return;
        }
        // Query userId list
        List<Long> userIds = blogList.stream()
                .map(Blog::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (CollUtil.isEmpty(userIds)) {
            return;
        }

        // Batch query user info
        List<com.smartLive.user.api.domain.UserDTO> userList = remoteAppUserService.getUserList(userIds);
        
        if (CollUtil.isEmpty(userList)) {
            return;
        }

        Map<Long, com.smartLive.user.api.domain.UserDTO> userMap = userList.stream().collect(Collectors.toMap(
                com.smartLive.user.api.domain.UserDTO::getId,
                Function.identity(),
                (v1, v2) -> v1
        ));

        blogList.forEach(blog -> {
            com.smartLive.user.api.domain.UserDTO user = userMap.get(blog.getUserId());
            if (user != null) {
                blog.setName(user.getNickName());
                blog.setIcon(user.getIcon());
            }
        });
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
     * 获取博客总数
     *
     * @return
     */
    @Override
    public Integer getBlogTotal() {
        return query().count().intValue();
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

        // 直接转换，超出范围会截断
        return count.intValue();
    }

    /**
     * 查询用户博客获得点赞数量
     *
     * @param userId
     * @return
     */
    @Override
    public Integer getLikeCount(Long userId) {
        List<Blog> blogList = query().eq("user_id", userId).list();
        return blogList.stream().mapToInt(Blog::getLiked).sum();
    }

    /**
     * 获取博客点赞数
     *
     * @param sourceId
     * @return
     */
    @Override
    public Integer getBlogLikeCount(Long sourceId) {
        return query()
                .select("liked")
                .eq("id", sourceId).one()
                .getLiked();
    }

    /**
     * 获取博客收藏数
     *
     * @param sourceId
     * @return
     */
    @Override
    public Integer getBlogStarCount(Long sourceId) {
        return query()
                .select("stared")
                .eq("id", sourceId)
                .one()
                .getStared();
    }

    /**
     * 查询分类下的博客
     *
     * @param typeId
     * @param current
     * @return
     */
    @Override
    public List<BlogVO> queryBlogByCategory(Long typeId, Integer current) {
        //从redis查询分类博客
        String key= RedisConstants.CACHE_BLOG_TYPE_KEY + typeId+":"+ current;
        List<Blog> blogList = getBlogListFromRedis(key);
        if (blogList != null) {
            queryBlogListIsLike(blogList);
            return convertToBlogVOList(blogList);
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
            // 查询blog有关的用户信息
            queryBlogListUserMessage(blogList);
            queryBlogListIsLike(blogList);
            //把查询结果写入redis
            redisService.setCacheObject(key, JSONUtil.toJsonStr(blogList), RedisConstants.CACHE_HOT_BLOG_TTL, TimeUnit.DAYS);
        }
        return convertToBlogVOList(blogList);
    }

    /**
     * 更新博客状态
     *
     * @param targetId
     * @param status
     * @return
     */
    @Override
    public Boolean updateBlogStatus(Long targetId, Integer status) {
        boolean updated = update(new UpdateWrapper<Blog>().set("status", status).eq("id", targetId));
        if (updated) {
            flashRedisBlogCache(targetId);
            flashRedisBlogListCache();
        }
        return updated;
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
                queryBlogListUserMessage(blogs);
                // 创建请求并发送
                sendEsBatchMessage(blogs);
                log.info("线程{}，发送第 {} 页，{} 条数据",Thread.currentThread().getName(),finalPage, blogs.size());
            });
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
        if (ids == null || ids.length == 0) {
            return "参数为空";
        }
        // Convert to Long list
        List<Long> idList = Arrays.stream(ids)
                .map(Long::valueOf)
                .collect(Collectors.toList());

        executorService.submit(() -> {
            log.info("线程{}，开始批量发布博客：{}", Thread.currentThread().getName(), idList);
            // Batch query
            List<Blog> blogs = query().in("id", idList).list();
            if (CollUtil.isNotEmpty(blogs)) {
                // Batch populate user info
                queryBlogListUserMessage(blogs);
                // Batch send message
                sendEsBatchMessage(blogs);
            }
        });
        return "发布成功";
    }

    /**
     * 批量发送ES同步消息
     * @param blogs
     */
    private void sendEsBatchMessage(List<Blog> blogs) {
        if (CollUtil.isEmpty(blogs)) {
            return;
        }
        ContentBatchSyncMessage request = new ContentBatchSyncMessage();
        request.setIndexName(EsIndexNameConstants.BLOG_INDEX_NAME);
        request.setData(blogs);
        request.setType(GlobalBizTypeEnum.BLOG.getCode());
        MqMessageSendUtils.sendMqMessage(rabbitTemplate,
                MqConstants.ES_EXCHANGE,
                MqConstants.ES_ROUTING_BATCH_INSERT,
                request);
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
