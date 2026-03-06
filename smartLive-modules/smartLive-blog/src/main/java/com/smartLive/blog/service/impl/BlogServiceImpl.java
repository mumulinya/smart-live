package com.smartLive.blog.service.impl;
import com.smartLive.common.core.constant.mq.InteractionMqConstants;
import com.smartLive.common.core.constant.mq.SearchMqConstants;
import com.smartLive.common.core.constant.mq.AiAuditMqConstants;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.blog.domain.Blog;
import com.smartLive.blog.domain.VO.BlogVO;
import com.smartLive.blog.mapper.BlogMapper;
import com.smartLive.blog.service.IBlogService;
import com.smartLive.common.core.domain.AppLoginUser;
import com.smartLive.common.rabbitmq.domain.*;
import org.springframework.beans.BeanUtils;
import com.smartLive.common.core.constant.*;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.enums.AuditStatusEnum;
import com.smartLive.common.core.enums.FeedTypeEnum;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.common.redis.util.CacheClient;
import com.smartLive.common.redis.util.ZSetIdManager;
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
    private static final long BLOG_LIST_ZSET_SLOT = 0L;

    @Autowired
    private BlogMapper blogMapper;
    
    @Autowired
    private MqMessageSendUtils mqMessageSendUtils;
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
    @Autowired
    private ZSetIdManager zSetIdManager;

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
               mqMessageSendUtils.sendMqMessage(SearchMqConstants.ES_SYNC_EXCHANGE,SearchMqConstants.ES_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
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
            mqMessageSendUtils.sendMqMessage(SearchMqConstants.ES_SYNC_EXCHANGE,SearchMqConstants.ES_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
            //更新redis缓存
            flashRedisBlogCache(id);
            flashRedisBlogListCache();
        }
        return i;
    }

    /**
     * 根据博客ID查询博文详情
     *
     * @param id 博客主键
     * @return 博客详情VO
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
        BlogVO blogVO = convertToBlogVO(blog);
        queryBlogUser(blogVO);
        isBlogLiked(blogVO);
        isBlogStared(blogVO);
        return blogVO;
    }
    /**
     * 查询最热博客列表
     *
     * @param current 当前页码
     * @return 热门博客列表
     */
    @Override

    public List<BlogVO> queryHotBlog(Integer current) {
        // 从最新的 ZSet 热度排行榜中获取博客 ID 极其分页数据
        Page<Long> longPage = zSetIdManager.pageIds(RedisConstants.BLOG_HOT_RANK_KEY, null, current, SystemConstants.MAX_PAGE_SIZE);
        List<Long> blogIdList = longPage.getRecords();

        if (CollUtil.isNotEmpty(blogIdList)) {
            return getBlogListByIds(blogIdList);
        }

        // ZSet 击穿或尚无数据时的兜底：查出全量数据写入 ZSet，再手动分页返回
        List<Blog> dbList = query()
                .ne("status","2")
                .ne("status","3")
                .orderByDesc("liked")
                .orderByDesc("create_time")
                .list();

        List<Blog> blogList = new ArrayList<>();
        if (CollUtil.isNotEmpty(dbList)) {
            final List<Blog> finalDbList = dbList;
            executorService.execute(() -> {
                log.info("重建博客热榜 ZSet");
                zSetIdManager.saveToZSet(RedisConstants.BLOG_HOT_RANK_KEY, finalDbList, Blog::getId, Blog::getCreateTime);

                // 推进计算队列等候定时长函数处理真正的衰减和融合热度分
                if (RedisConstants.BLOG_CALC_QUEUE_KEY != null) {
                    redisService.setCacheSet(RedisConstants.BLOG_CALC_QUEUE_KEY, finalDbList.stream().map(b -> String.valueOf(b.getId())).collect(Collectors.toSet()));
                }
            });

            // 手动分页截取当前页数据
            int start = (current - 1) * SystemConstants.MAX_PAGE_SIZE;
            int pageSize = SystemConstants.MAX_PAGE_SIZE;
            if (dbList.size() > start) {
                blogList = dbList.subList(start, Math.min(start + pageSize, dbList.size()));
            }
        }

        List<BlogVO> voList = convertToBlogVOList(blogList);
        queryBlogListUserMessage(voList);
        queryBlogListIsLike(voList);
        return voList;
    }


    /**
     * 保存博客（发布/草稿）
     *
     * @param blog 博客实体
     * @return 博客ID
     */
    @Override
    public Long saveBlog(Blog blog) {
        blog.setUserId(UserContextHolder.getUser().getId());
        blog.setCreateTime(DateUtils.getNowDate());
       if (blog.getShopId() != null && !blog.getShopId().toString().isEmpty()) {
           String firstShopIdStr = blog.getShopId().toString().split(",")[0];
           Long firstShopId = Long.valueOf(firstShopIdStr);
           ShopDTO shopDTO = remoteShopService.getShopById(firstShopId);
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
        mqMessageSendUtils.sendMqMessage(InteractionMqConstants.INTERACT_FEED_EXCHANGE, InteractionMqConstants.INTERACT_FEED_ROUTING_KEY, feedEventMessage);
        //添加es数据
        publish(new String[]{blog.getId().toString()});
        //添加用户es数据
        String actionType=UserResourceActionTypeConstants.USER_RESOURCE_ACTION_PUBLISH;
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
        mqMessageSendUtils.sendMqMessage( SearchMqConstants.ES_SYNC_EXCHANGE,SearchMqConstants.ES_SYNC_USER_RESOURCE_INSERT_ROUTING_KEY, userResourceMessage);
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
        mqMessageSendUtils.sendMqMessage( AiAuditMqConstants.AUDIT_DIRECT_EXCHANGE,AiAuditMqConstants.AUDIT_ROUTING_KEY, auditMessage);
    }

    /**
     * 查询我的博客列表
     *
     * @param b       博客查询条件（包含状态等筛选参数）
     * @param current 当前页码
     * @return 我的博客列表
     */
    @Override
    public List<BlogVO> queryMyBlog(Blog b,Integer current) {
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            throw new BusinessException("未登录");
        }
        // 根据用户查询
        Page<Blog> page = query()
                .eq("user_id", user.getId())
                .orderByDesc("pin")
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> blogList = page.getRecords();
        List<BlogVO> voList = convertToBlogVOList(blogList);
        queryBlogListUserMessage(voList);
        queryBlogListIsLike(voList);
        return voList;
    }
    /**
     * 查询指定用户发布的博客列表
     *
     * @param current 当前页码
     * @param userId  用户ID
     * @return 博客列表
     */
    @Override
    public List<BlogVO> queryBlogByUserId(Integer current, Long userId) {
        Page<Blog> page = query()
                .eq("user_id", userId)
                .eq("status",0)
                .orderByDesc("pin")
                .orderByAsc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Blog> records = page.getRecords();
        List<BlogVO> voList = convertToBlogVOList(records);
        queryBlogListIsLike(voList);
        return voList;
    }
    /**
     * 查询博客详情（包含用户信息）
     *
     * @param id 博客主键
     * @return 博客详情VO
     */
    @Override
    public BlogVO getBlogById(Long id) {
        Blog blog = query().eq("id", id).one();
        BlogVO blogVO = convertToBlogVO(blog);
        queryBlogUser(blogVO);
        return blogVO;
    }

    /**
     * 根据ID列表批量获取博客（含用户信息、点赞状态）
     *
     * @param sourceIdList 博客ID列表
     * @return 博客列表
     */
    @Override
    public List<BlogVO> getBlogListByIds(List<Long> sourceIdList) {
        if (CollUtil.isEmpty(sourceIdList)) {
            return Collections.emptyList();
        }
        List<Blog> blogList = redisMultiCacheManager.queryBatchWithCache(
                RedisConstants.CACHE_BLOG_KEY,
                sourceIdList,
                Blog.class,
                missingIds -> query().in("id", missingIds).list(),
                Blog::getId,
                RedisConstants.CACHE_BLOG_TTL,
                TimeUnit.MINUTES
        );
        if (CollUtil.isEmpty(blogList)) {
            return Collections.emptyList();
        }
        Map<Long, Blog> blogMap = blogList.stream()
                .filter(blog -> blog != null && blog.getId() != null)
                .collect(Collectors.toMap(Blog::getId, Function.identity(), (v1, v2) -> v1));
        List<Blog> orderedBlogList = new ArrayList<>(sourceIdList.size());
        for (Long blogId : sourceIdList) {
            Blog blog = blogMap.get(blogId);
            if (blog != null) {
                orderedBlogList.add(blog);
            }
        }
        if (CollUtil.isEmpty(orderedBlogList)) {
            return Collections.emptyList();
        }
        List<BlogVO> voList = convertToBlogVOList(orderedBlogList);
        queryBlogListUserMessage(voList);
        queryBlogListIsLike(voList);
        return voList;
    }

    /**
     * 批量查询博客是否被点赞
     * @param blogList
     */
    private void queryBlogListIsLike(List<BlogVO> blogVOList) {
        if (CollUtil.isEmpty(blogVOList)) {
            return;
        }
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            blogVOList.forEach(vo -> {
                if (vo != null) {
                    vo.setIsLike(false);
                }
            });
            return;
        }
        // Extract IDs
        List<Long> blogIds = blogVOList.stream()
                .map(BlogVO::getId)
                .collect(Collectors.toList());

        // Batch check Likes
        LikeDTO likeDTO = new LikeDTO();
        likeDTO.setUserId(user.getId());
        likeDTO.setSourceType(GlobalBizTypeEnum.BLOG.getCode());
        Map<Long, Boolean> likeMap = remoteLikeService.getIsLikeBatch(likeDTO, blogIds);
        log.info("likeMap: {}", likeMap);
        blogVOList.forEach(vo -> {
            if (vo != null) {
                vo.setIsLike(likeMap.getOrDefault(vo.getId(), false));
            }
        });
    }

    /**
     * 批量查询博客是否被收藏
     * @param blogList
     */
    private void queryBlogListIsStar(List<BlogVO> blogVOList) {
        if (CollUtil.isEmpty(blogVOList)) {
            return;
        }
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            blogVOList.forEach(vo -> {
                if (vo != null) {
                    vo.setIsStared(false);
                }
            });
            return;
        }
        // Extract IDs
        List<Long> blogIds = blogVOList.stream()
                .map(BlogVO::getId)
                .collect(Collectors.toList());

        // Batch check Stars
        StarDTO starDTO = new StarDTO();
        starDTO.setUserId(user.getId());
        starDTO.setSourceType(GlobalBizTypeEnum.BLOG.getCode());
        Map<Long, Boolean> starMap = remoteStarService.getIsStarBatch(starDTO, blogIds);

        blogVOList.forEach(vo -> {
            if (vo != null) {
                vo.setIsStared(starMap.getOrDefault(vo.getId(), false));
            }
        });
    }

    /**
     * 批量查询博客有关的用户信息
     * @param blogList
     */
    private void queryBlogListUserMessage(List<BlogVO> blogVOList) {
        if (CollUtil.isEmpty(blogVOList)) {
            return;
        }
        // Query userId list
        List<Long> userIds = blogVOList.stream()
                .map(BlogVO::getUserId)
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

        blogVOList.forEach(vo -> {
            com.smartLive.user.api.domain.UserDTO user = userMap.get(vo.getUserId());
            if (user != null) {
                vo.setName(user.getNickName());
                vo.setIcon(user.getIcon());
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
     * 批量更新博客点赞数
     *
     * @param updateMap 博客ID与点赞数的映射
     * @return 更新结果
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
     * 批量更新博客评论数
     *
     * @param updateMap 博客ID与评论数的映射
     * @return 更新结果
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
     * 批量更新博客收藏数
     *
     * @param updateMap 博客ID与收藏数的映射
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
        flashCache();
        return true;
    }

    /**
     * 获取博客总数
     *
     * @return 博客总数
     */
    @Override
    public Integer getBlogTotal() {
        return query().count().intValue();
    }
    /**
     * 查询用户博客数量
     *
     * @param userId 用户ID
     * @return 博客数量
     */
    @Override
    public Integer getBlogCount(Long userId) {
        //查询数量
        Long count = query().eq("user_id", userId).count();

        // 直接转换，超出范围会截断
        return count.intValue();
    }

    /**
     * 查询用户博客获得的总点赞数
     *
     * @param userId 用户ID
     * @return 点赞总数
     */
    @Override
    public Integer getLikeCount(Long userId) {
        List<Blog> blogList = query().eq("user_id", userId).list();
        return blogList.stream().mapToInt(Blog::getLiked).sum();
    }

    /**
     * 获取博客点赞数
     *
     * @param sourceId 博客ID
     * @return 点赞数量
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
     * @param sourceId 博客ID
     * @return 收藏数量
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
     * 查询指定分类下的博客列表
     *
     * @param typeId  分类ID
     * @param current 当前页码
     * @return 博客列表
     */
    @Override
    public List<BlogVO> queryBlogByCategory(Long typeId, Integer current) {
        //从redis查询分类博客
        String key= RedisConstants.CACHE_BLOG_TYPE_KEY + typeId+":"+ current;
        List<Long> blogIdList = getBlogIdListFromRedis(key, SystemConstants.MAX_PAGE_SIZE);
        if (CollUtil.isNotEmpty(blogIdList)) {
            return getBlogListByIds(blogIdList);
        }
        Page<Blog> page = query()
                .select("images","liked","user_id","title","id")
                .eq("type_id", typeId)
                .ne("status","2")
                .ne("status","3")
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> blogList = page.getRecords();
        List<BlogVO> voList = convertToBlogVOList(blogList);
        if(blogList!= null&&blogList.size()>0){
            // 查询blog有关的用户信息
            queryBlogListUserMessage(voList);
            queryBlogListIsLike(voList);
            //把查询结果写入redis
            saveBlogIdListToRedis(key, blogList, RedisConstants.CACHE_HOT_BLOG_TTL, TimeUnit.DAYS);
        }
        return voList;
    }

    /**
     * 更新博客状态（审核通过/拒绝）
     *
     * @param targetId 博客ID
     * @param status   博客状态
     * @return 更新结果
     */
    @Override
    public Boolean updateBlogStatus(Long targetId, Integer status, String reason) {
        // 拒绝时写入拒绝原因，通过时清空拒绝原因
        String rejectReason = AuditStatusEnum.isRejected(status) ? reason : null;
        boolean updated = update(new UpdateWrapper<Blog>().set("status", status).set("reject_reason", rejectReason).eq("id", targetId));
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
                List<BlogVO> voList = convertToBlogVOList(blogs);
                queryBlogListUserMessage(voList);
                // 创建请求并发送
                sendEsBatchMessage(voList);
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
                List<BlogVO> voList = convertToBlogVOList(blogs);
                // Batch populate user info
                queryBlogListUserMessage(voList);
                // Batch send message
                sendEsBatchMessage(voList);
            }
        });
        return "发布成功";
    }

    /**
     * 批量发送ES同步消息
     * @param blogs
     */
    private void sendEsBatchMessage(List<?> blogs) {
        if (CollUtil.isEmpty(blogs)) {
            return;
        }
        ContentBatchSyncMessage request = new ContentBatchSyncMessage();
        request.setIndexName(EsIndexNameConstants.BLOG_INDEX_NAME);
        request.setData(blogs);
        request.setType(GlobalBizTypeEnum.BLOG.getCode());
        mqMessageSendUtils.sendMqMessage(
                SearchMqConstants.ES_SYNC_EXCHANGE,
                SearchMqConstants.ES_SYNC_BATCH_INSERT_ROUTING_KEY,
                request);
    }
    /**
     * 刷新博客缓存（详情 + 列表 + 分类）
     *
     * @return 刷新结果
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
     * @param blogVO
     */
    private void queryBlogUser(BlogVO blogVO){
        Long userId = blogVO.getUserId();
        //根据用户id获取用户信息
        com.smartLive.user.api.domain.UserDTO user= remoteAppUserService.queryUserById(userId);
        if (user != null) {
            blogVO.setName(user.getNickName());
            blogVO.setIcon(user.getIcon());
        }
    }
    /**
     * 判断当前用户是否已经点赞
     * @param blog
     */
    private void isBlogLiked(BlogVO blogVO) {
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            //未登录,不用查询是否点赞
            blogVO.setIsLike(false);
            return;
        }
        //获取当前登录用户
        Long userId = user.getId();
        LikeDTO likeDTO = new LikeDTO();
        likeDTO.setUserId(userId);
        likeDTO.setSourceId(blogVO.getId());
        likeDTO.setSourceType(GlobalBizTypeEnum.BLOG.getCode());
        //判断当前用户是否已经点赞
        Boolean isLike = remoteLikeService.isLike(likeDTO);
        blogVO.setIsLike(isLike);
    }

    /**
     * 判断当前用户是否已经收藏博客
     * @param blog
     */
    private void isBlogStared(BlogVO blogVO) {
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            //未登录,不用查询是否收藏
            blogVO.setIsStared(false);
            return;
        }
        //获取当前登录用户
        Long userId = user.getId();
        StarDTO starDTO = new StarDTO();
        starDTO.setUserId(userId);
        starDTO.setSourceId(blogVO.getId());
        starDTO.setSourceType(GlobalBizTypeEnum.BLOG.getCode());
        //判断当前用户是否已经收藏
        Boolean isStared = remoteStarService.isStar(starDTO);
        blogVO.setIsStared(isStared);
    }
    /**
     * 从 Redis ZSet 中获取博客ID列表
     *
     * @param key  缓存键
     * @param size 获取数量
     * @return 博客ID列表
     */
    private List<Long> getBlogIdListFromRedis(String key, long size) {
        Page<Long> idPage = zSetIdManager.pageIds(buildBlogListZSetPrefix(key), BLOG_LIST_ZSET_SLOT, 1L, size);
        if (idPage == null || CollUtil.isEmpty(idPage.getRecords())) {
            return Collections.emptyList();
        }
        return idPage.getRecords();
    }

    /**
     * 将博客ID列表以ZSet形式保存到Redis缓存
     *
     * @param key     缓存键
     * @param blogList 博客列表
     * @param timeout  过期时间
     * @param unit     时间单位
     */
    private void saveBlogIdListToRedis(String key, List<Blog> blogList, long timeout, TimeUnit unit) {
        if (key == null || key.isEmpty() || CollUtil.isEmpty(blogList)) {
            return;
        }
        long scoreSeed = System.currentTimeMillis();
        List<BlogListRankItem> rankItems = new ArrayList<>(blogList.size());
        for (int i = 0; i < blogList.size(); i++) {
            Blog blog = blogList.get(i);
            if (blog == null || blog.getId() == null) {
                continue;
            }
            rankItems.add(new BlogListRankItem(blog.getId(), new Date(scoreSeed - i)));
        }
        if (CollUtil.isEmpty(rankItems)) {
            return;
        }
        String zSetKey = buildBlogListZSetKey(key);
        redisService.deleteObject(key);
        redisService.deleteObject(zSetKey);
        zSetIdManager.saveToZSet(zSetKey, rankItems, BlogListRankItem::getId, BlogListRankItem::getScoreTime);
        redisService.expire(zSetKey, timeout, unit);
    }

    /**
     * 构建博客列表ZSet缓存键前缀
     *
     * @param key 原始缓存键
     * @return ZSet键前缀
     */
    private String buildBlogListZSetPrefix(String key) {
        return key + ":";
    }

    /**
     * 构建博客列表ZSet缓存完整键名
     *
     * @param key 原始缓存键
     * @return ZSet完整键名
     */
    private String buildBlogListZSetKey(String key) {
        return buildBlogListZSetPrefix(key) + BLOG_LIST_ZSET_SLOT;
    }

    /**
     * 博客列表排行项，用于ZSet缓存排序
     * 封装博客ID与评分时间，供ZSet写入时使用
     */
    private static class BlogListRankItem {
        private final Long id;
        private final Date scoreTime;

        private BlogListRankItem(Long id, Date scoreTime) {
            this.id = id;
            this.scoreTime = scoreTime;
        }

        private Long getId() {
            return id;
        }

        private Date getScoreTime() {
            return scoreTime;
        }
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
