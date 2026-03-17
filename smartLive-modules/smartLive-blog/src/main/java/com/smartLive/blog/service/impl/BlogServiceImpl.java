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
import com.smartLive.common.core.enums.ContentStatusEnum;
import com.smartLive.common.core.enums.common.AuditStatusEnum;
import com.smartLive.common.core.enums.interaction.FeedTypeEnum;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import cn.hutool.core.util.StrUtil;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.smartLive.common.redis.util.RedisMultiCacheManager;

/**
 * 博客服务实现类，负责博客发布、查询、缓存与索引同步等业务。
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
     * 将博客实体转换为视图对象。
     *
     * @param blog 博客信息
     * @return 博客视图对象
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
     * 批量转换博客实体为视图对象列表。
     *
     * @param blogList 博客列表
     * @return 博客视图对象列表
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
     * 根据ID查询博客实体。
     *
     * @param id 博客ID
     * @return 博客实体
     */
    @Override
    public Blog selectBlogById(Long id)
    {
        return blogMapper.selectBlogById(id);
    }

    /**
     * 查询博客实体列表。
     *
     * @param blog 博客信息
     * @return 博客列表
     */
    @Override
    public List<Blog> selectBlogList(Blog blog)
    {
        return blogMapper.selectBlogList(blog);
    }

    /**
     * 根据ID查询博客视图详情。
     *
     * @param id 博客ID
     * @return 博客详情
     */
    @Override
    public BlogVO selectBlogVoById(Long id)
    {
        Blog blog = blogMapper.selectBlogById(id);
        BlogVO blogVO = convertToBlogVO(blog);
        if (blogVO == null) {
            return null;
        }
        fillBlogAdminNames(Collections.singletonList(blogVO));
        return blogVO;
    }

    /**
     * 查询博客视图列表。
     *
     * @param blog 博客信息
     * @return 博客列表
     */
    @Override
    public List<BlogVO> selectBlogVoList(Blog blog)
    {
        List<Blog> blogList = blogMapper.selectBlogList(blog);
        List<BlogVO> blogVOList = convertToBlogVOList(blogList);
        fillBlogAdminNames(blogVOList);
        return blogVOList;
    }

    /**
     * 新增博客并处理发布与缓存。
     *
     * @param blog 博客信息
     * @return 影响行数
     */
    @Override
    public int insertBlog(Blog blog)
    {
        blog.setCreateTime(DateUtils.getNowDate());
        int i = blogMapper.insertBlog(blog);
        if(i > 0){
            publish(new String[]{blog.getId().toString()});
            redisService.deleteObject(RedisConstants.CACHE_HOT_BLOG_KEY+blog.getTypeId());
        }
        return i;
    }

    /**
     * 更新博客并同步审核与缓存。
     *
     * @param blog 博客信息
     * @return 影响行数
     */
    @Override
    public int updateBlog(Blog blog)
    {
        blog.setUpdateTime(DateUtils.getNowDate());
        int i = blogMapper.updateBlog(blog);
        if(i > 0){
            publish(new String[]{blog.getId().toString()});
            blog=getById(blog.getId());
            sendAuditMessage(blog);
            flashRedisBlogCache(blog.getId());
            flashRedisBlogListCache();
        }
        return i;
    }

    /**
     * 批量删除博客并同步索引与缓存。
     *
     * @param ids 博客ID数组
     * @return 影响行数
     */
    @Override
    public int deleteBlogByIds(Long[] ids)
    {
        int i = blogMapper.deleteBlogByIds(ids);
        if (i > 0) {
        CountDownLatch latch=new CountDownLatch(ids.length);
        for (Long id : ids) {
            executorService.submit(()->{
               log.info("Deleting blog id {} from search indexes", id);
               sendBlogDeleteSyncMessage(id);
               flashRedisBlogCache(id);
               latch.countDown();
           });
        }
        try {
            log.info("Waiting for blog delete tasks to finish");
            latch.await();
            log.info("Blog delete tasks finished, refreshing cache");
            flashRedisBlogListCache();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        }
        return 1;
    }

    /**
     * 删除单个博客并同步索引与缓存。
     *
     * @param id 博客ID
     * @return 影响行数
     */
    @Override
    public int deleteBlogById(Long id)
    {
        int i = blogMapper.deleteBlogById(id);
        if(i > 0){
            sendBlogDeleteSyncMessage(id);
            flashRedisBlogCache(id);
            flashRedisBlogListCache();
        }
        return i;
    }

    /**
     * 查询博客详情并补充用户与互动状态。
     *
     * @param id 博客ID
     * @return 博客详情
     */
    @Override
    public BlogVO queryBlogById(Long id) {
        Blog blog = cacheClient.queryWithLogicalExpireAndPassThrough(
                RedisConstants.CACHE_BLOG_KEY,
                RedisConstants.LOCK_BLOG_KEY,
                id,
                Blog.class,
                blogId -> query()
                        .eq("id", blogId)
                        .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                        .eq("audit_status", AuditStatusEnum.PASS.getCode())
                        .one(),
                RedisConstants.CACHE_BLOG_TTL,
                TimeUnit.MINUTES
        );
        if (blog == null) {
            throw new BusinessException("blog not found");
        }
        BlogVO blogVO = convertToBlogVO(blog);
        queryBlogUser(blogVO);
        isBlogLiked(blogVO);
        isBlogStared(blogVO);
        return blogVO;
    }
    /**
     * 分页查询热门博客列表。
     *
     * @param current 当前页码
     * @return 博客列表
     */
    @Override
    public List<BlogVO> queryHotBlog(Integer current) {
        Page<Long> longPage = zSetIdManager.pageIds(RedisConstants.BLOG_HOT_RANK_KEY, null, current, SystemConstants.MAX_PAGE_SIZE);
        List<Long> blogIdList = longPage.getRecords();

        if (CollUtil.isNotEmpty(blogIdList)) {
            return getBlogListByIds(blogIdList);
        }else if(current==1){
            List<Blog> dbList = query()
                    .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .orderByDesc("liked")
                    .orderByDesc("create_time")
                    .list();

            List<Blog> blogList = new ArrayList<>();
            if (CollUtil.isNotEmpty(dbList)) {
                final List<Blog> finalDbList = dbList;
                executorService.execute(() -> {
                    log.info("开始写入博客热榜 ZSet 缓存");
                    zSetIdManager.saveToZSet(RedisConstants.BLOG_HOT_RANK_KEY, finalDbList, Blog::getId, Blog::getCreateTime);

                    if (RedisConstants.BLOG_CALC_QUEUE_KEY != null) {
                        redisService.setCacheSet(RedisConstants.BLOG_CALC_QUEUE_KEY, finalDbList.stream().map(b -> String.valueOf(b.getId())).collect(Collectors.toSet()));
                    }
                });

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
        return Collections.emptyList();
    }


    /**
     * 保存博客并发布相关消息。
     *
     * @param blog 博客信息
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
        boolean success = save(blog);
        if (!success) {
            throw new BusinessException("failed to save blog");
        }
        if (blog.getStatus() != null && blog.getStatus().intValue() == ContentStatusEnum.DRAFT.getCode()) {
            return blog.getId();
        }
        FeedEventMessage feedEventMessage = FeedEventMessage.builder()
                .feedType(FeedTypeEnum.USER_FEED.getCode())
                .sourceType(GlobalBizTypeEnum.USER.getCode())
                .sourceId(blog.getUserId())
                .bizType(GlobalBizTypeEnum.BLOG.getCode())
                .bizId(blog.getId())
                .publishTime(blog.getCreateTime())
                .build();
        mqMessageSendUtils.sendMqMessage(InteractionMqConstants.INTERACT_FEED_EXCHANGE, InteractionMqConstants.INTERACT_FEED_ROUTING_KEY, feedEventMessage);
        publish(new String[]{blog.getId().toString()});
        String actionType=UserResourceActionTypeConstants.USER_RESOURCE_ACTION_PUBLISH;
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
        redisService.deleteObject(RedisConstants.CACHE_HOT_BLOG_KEY+blog.getTypeId());
        return blog.getId();
    }

    /**
     * 发送博客审核消息。
     *
     * @param blog 博客信息
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
     * 分页查询当前用户博客列表。
     *
     * @param b 博客查询条件
     * @param current 当前页码
     * @return 博客列表
     */
    @Override
    public List<BlogVO> queryMyBlog(Blog b,Integer current) {
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            throw new BusinessException("user not logged in");
        }
        Page<Blog> page = query()
                .eq("user_id", user.getId())
                .orderByDesc("pin")
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Blog> blogList = page.getRecords();
        List<BlogVO> voList = convertToBlogVOList(blogList);
        queryBlogListUserMessage(voList);
        queryBlogListIsLike(voList);
        return voList;
    }
    /**
     * 分页查询指定用户博客列表。
     *
     * @param current 当前页码
     * @param userId 用户ID
     * @return 博客列表
     */
    @Override
    public List<BlogVO> queryBlogByUserId(Integer current, Long userId) {
        Page<Blog> page = query()
                .eq("user_id", userId)
                .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .orderByDesc("pin")
                .orderByAsc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Blog> records = page.getRecords();
        List<BlogVO> voList = convertToBlogVOList(records);
        queryBlogListIsLike(voList);
        return voList;
    }
    /**
     * 根据ID查询博客详情（内部使用）。
     *
     * @param id 博客ID
     * @return 博客详情
     */
    @Override
    public BlogVO getBlogById(Long id) {
        Blog blog = query().eq("id", id).one();
        BlogVO blogVO = convertToBlogVO(blog);
        queryBlogUser(blogVO);
        return blogVO;
    }

    /**
     * 按ID批量查询博客并保持顺序。
     *
     * @param sourceIdList 博客ID列表
     * @return 博客列表
     */
    @Override
    public List<BlogVO> getBlogListByIds(List<Long> sourceIdList) {
        log.info("getBlogListByIds: {}", sourceIdList);
        if (CollUtil.isEmpty(sourceIdList)) {
            return Collections.emptyList();
        }
        List<Blog> blogList = redisMultiCacheManager.queryBatchWithCache(
                RedisConstants.CACHE_BLOG_KEY,
                RedisConstants.LOCK_BLOG_KEY,
                sourceIdList,
                Blog.class,
                missingIds -> query()
                        .in("id", missingIds)
                        .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                        .eq("audit_status", AuditStatusEnum.PASS.getCode())
                        .list(),
                Blog::getId,
                RedisConstants.CACHE_BLOG_TTL,
                TimeUnit.MINUTES
        );
        log.info("getBlogListByIds: {}", blogList);
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
     * 批量填充点赞状态。
     *
     * @param blogVOList 博客视图列表
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
        // 提取博客ID列表
        List<Long> blogIds = blogVOList.stream()
                .map(BlogVO::getId)
                .collect(Collectors.toList());

        // 批量查询点赞状态
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
     * 批量填充收藏状态。
     *
     * @param blogVOList 博客视图列表
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
        // 提取博客ID列表
        List<Long> blogIds = blogVOList.stream()
                .map(BlogVO::getId)
                .collect(Collectors.toList());

        // 批量查询收藏状态
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
     * 批量填充作者信息。
     *
     * @param blogVOList 博客视图列表
     */
    private void queryBlogListUserMessage(List<BlogVO> blogVOList) {
        fillBlogUserNames(blogVOList);
    }

    /**
     * 填充作者与店铺名称。
     *
     * @param blogVOList 博客视图列表
     */
    private void fillBlogAdminNames(List<BlogVO> blogVOList) {
        fillBlogUserNames(blogVOList);
        fillBlogShopNames(blogVOList);
    }

    /**
     * 填充作者昵称与头像。
     *
     * @param blogVOList 博客视图列表
     */
    private void fillBlogUserNames(List<BlogVO> blogVOList) {
        if (CollUtil.isEmpty(blogVOList)) {
            return;
        }
        List<BlogVO> validBlogVOList = blogVOList.stream()
                .filter(Objects::nonNull)
                .toList();
        if (CollUtil.isEmpty(validBlogVOList)) {
            return;
        }
        List<Long> userIds = validBlogVOList.stream()
                .map(BlogVO::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        List<com.smartLive.user.api.domain.UserDTO> userList = remoteAppUserService.getUserList(userIds);
        if (CollUtil.isEmpty(userList)) {
            return;
        }
        Map<Long, com.smartLive.user.api.domain.UserDTO> userMap = userList.stream().collect(Collectors.toMap(
                com.smartLive.user.api.domain.UserDTO::getId,
                Function.identity(),
                (v1, v2) -> v1
        ));
        validBlogVOList.forEach(vo -> {
            com.smartLive.user.api.domain.UserDTO user = userMap.get(vo.getUserId());
            if (user != null) {
                String nickName = user.getNickName() == null ? "" : user.getNickName();
                vo.setName(nickName);
                vo.setUserName(nickName);
                vo.setIcon(user.getIcon());
            }
        });
    }

    /**
     * 填充店铺名称。
     *
     * @param blogVOList 博客视图列表
     */
    private void fillBlogShopNames(List<BlogVO> blogVOList) {
        if (CollUtil.isEmpty(blogVOList)) {
            return;
        }
        List<BlogVO> validBlogVOList = blogVOList.stream()
                .filter(Objects::nonNull)
                .toList();
        if (CollUtil.isEmpty(validBlogVOList)) {
            return;
        }
        List<Long> shopIds = validBlogVOList.stream()
                .map(BlogVO::getShopId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(shopIds)) {
            return;
        }
        List<ShopDTO> shopList = remoteShopService.getShopList(shopIds);
        if (CollUtil.isEmpty(shopList)) {
            return;
        }
        Map<Long, String> shopNameMap = shopList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ShopDTO::getId, shop -> shop.getName() == null ? "" : shop.getName(), (left, right) -> left));
        validBlogVOList.forEach(vo -> {
            if (vo.getShopId() != null) {
                vo.setShopName(shopNameMap.getOrDefault(vo.getShopId(), ""));
            }
        });
    }

    /**
     * 更新博客置顶状态。
     *
     * @param blog 博客信息
     * @return 执行结果
     */
    @Override
    public boolean isPin(Blog blog) {
        boolean update = this.lambdaUpdate()
                .eq(Blog::getId, blog.getId())
                .set(Blog::getPin, blog.getPin())
                .update();
        if (update) {
            flashRedisBlogCache(blog.getId());
        }
        return update;
    }

    /**
     * 批量更新点赞数。
     *
     * @param updateMap 更新映射
     * @return 执行结果
     */
    @Override
    public Boolean updateLikeCountBatch(Map<Long, Integer> updateMap) {
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
                baseMapper.updateLikeCountBatch(batchMap);
            }
        } else {
            baseMapper.updateLikeCountBatch(updateMap);
        }
        flashCache();
        return true;
    }

    /**
     * 批量更新评论数。
     *
     * @param updateMap 更新映射
     * @return 执行结果
     */
    @Override
    public Boolean updateCommentCountBatch(Map<Long, Integer> updateMap) {
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
                baseMapper.updateCommentCountBatch(batchMap);
            }
        } else {
            baseMapper.updateCommentCountBatch(updateMap);
        }
        flashCache();
        return true;
    }

    /**
     * 批量更新收藏数。
     *
     * @param updateMap 更新映射
     * @return 执行结果
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
        flashCache();
        return true;
    }

    /**
     * 获取博客总数。
     * @return 博客总数
     */
    @Override
    public Integer getBlogTotal() {
        return query().count().intValue();
    }
    /**
     * 获取用户博客数量。
     *
     * @param userId 用户ID
     * @return 博客数量
     */
    @Override
    public Integer getBlogCount(Long userId) {
        Long count = query().eq("user_id", userId).count();

        return count.intValue();
    }

    /**
     * 获取用户博客获赞总数。
     *
     * @param userId 用户ID
     * @return 获赞总数
     */
    @Override
    public Integer getLikeCount(Long userId) {
        List<Blog> blogList = query().eq("user_id", userId).list();
        return blogList.stream().mapToInt(Blog::getLiked).sum();
    }

    /**
     * 获取单篇博客点赞数。
     *
     * @param sourceId 博客ID
     * @return 点赞数
     */
    @Override
    public Integer getBlogLikeCount(Long sourceId) {
        return query()
                .select("liked")
                .eq("id", sourceId).one()
                .getLiked();
    }

    /**
     * 获取单篇博客收藏数。
     *
     * @param sourceId 博客ID
     * @return 收藏数
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
     * 按分类分页查询博客列表。
     *
     * @param typeId 分类ID
     * @param current 当前页码
     * @return 博客列表
     */
    @Override
    public List<BlogVO> queryBlogByCategory(Long typeId, Integer current) {
        String key= RedisConstants.CACHE_BLOG_TYPE_KEY + typeId+":"+ current;
        List<Long> blogIdList = getBlogIdListFromRedis(key, SystemConstants.MAX_PAGE_SIZE);
        if (CollUtil.isNotEmpty(blogIdList)) {
            return getBlogListByIds(blogIdList);
        }
        Page<Blog> page = query()
                .select("images","liked","user_id","title","id")
                .eq("type_id", typeId)
                .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Blog> blogList = page.getRecords();
        List<BlogVO> voList = convertToBlogVOList(blogList);
        if(blogList!= null&&blogList.size()>0){
            queryBlogListUserMessage(voList);
            queryBlogListIsLike(voList);
            saveBlogIdListToRedis(key, blogList, RedisConstants.CACHE_HOT_BLOG_TTL, TimeUnit.DAYS);
        }
        return voList;
    }

    /**
     * 更新博客审核状态并同步索引与缓存。
     *
     * @param targetId 博客ID
     * @param status 状态值
     * @param reason 审核原因
     * @return 执行结果
     */
    @Override
    public Boolean updateBlogStatus(Long targetId, Integer status, String reason) {
        String rejectReason = AuditStatusEnum.isRejected(status) ? reason : null;
        Short businessStatus = null;
        if (Objects.equals(status, AuditStatusEnum.PASS.getCode())) {
            businessStatus = ContentStatusEnum.PUBLISHED.getCode().shortValue();
        } else if (AuditStatusEnum.isRejected(status)) {
            businessStatus = ContentStatusEnum.OFF.getCode().shortValue();
        }
        UpdateWrapper<Blog> uw = new UpdateWrapper<Blog>()
                .set("audit_status", status)
                .set("reject_reason", rejectReason)
                .eq("id", targetId);
        if (businessStatus != null) {
            uw.set("status", businessStatus);
        }
        boolean updated = update(uw);
        if (updated) {
            flashRedisBlogCache(targetId);
            flashRedisBlogListCache();
            if (Objects.equals(status, AuditStatusEnum.PASS.getCode())) {
                publish(new String[]{targetId.toString()});
            } else if (AuditStatusEnum.isRejected(status)) {
                sendBlogDeleteSyncMessage(targetId);
            }
        }
        return updated;
    }

    /**
     * 全量发布博客数据到搜索索引。
     * @return 执行结果
     */
    @Override
    public String allPublish() {
        int page = PageConstants.PAGE_NUMBER;
        int pageSize =5;
        while (true) {
            List<Blog> blogs = query()
                    .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .page(new Page<>(page, pageSize))
                    .getRecords();
            if (blogs.isEmpty()) {
                break;
            }
            int finalPage = page;
            executorService.execute(() -> {
                List<BlogVO> voList = convertToBlogVOList(blogs);
                queryBlogListUserMessage(voList);
                sendBlogBatchSyncMessage(voList);
                log.info("Thread {} published blog page {}, size {}", Thread.currentThread().getName(), finalPage, blogs.size());
            });
            page++;
        }
        return "publish success";
    }

    /**
     * 发布指定博客数据到搜索索引。
     *
     * @param ids 博客ID数组
     * @return 执行结果
     */
    @Override
    public String publish(String[] ids) {
        if (ids == null || ids.length == 0) {
            return "no ids to publish";
        }
        // 转换为ID列表
        List<Long> idList = Arrays.stream(ids)
                .map(Long::valueOf)
                .collect(Collectors.toList());

        executorService.submit(() -> {
            log.info("Publishing blogs {} on thread {}", idList, Thread.currentThread().getName());
            // 批量查询博客
            List<Blog> blogs = query()
                    .in("id", idList)
                    .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .list();
            if (CollUtil.isNotEmpty(blogs)) {
                List<BlogVO> voList = convertToBlogVOList(blogs);
                // 批量填充用户信息
                queryBlogListUserMessage(voList);
                // 批量发送同步消息
                sendBlogBatchSyncMessage(voList);
            }
        });
        return "publish success";
    }

    /**
     * 批量发送博客同步消息。
     *
     * @param blogs 博客数据列表
     */
    private void sendBlogBatchSyncMessage(List<?> blogs) {
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
        mqMessageSendUtils.sendMqMessage(
                SearchMqConstants.MILVUS_SYNC_EXCHANGE,
                SearchMqConstants.MILVUS_SYNC_BATCH_INSERT_ROUTING_KEY,
                request);
    }

    /**
     * 发送博客删除同步消息。
     *
     * @param id 博客ID
     */
    private void sendBlogDeleteSyncMessage(Long id) {
        ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
        contentSyncMessage.setId(id);
        contentSyncMessage.setIndexName(EsIndexNameConstants.BLOG_INDEX_NAME);
        contentSyncMessage.setType(GlobalBizTypeEnum.BLOG.getCode());
        mqMessageSendUtils.sendMqMessage(
                SearchMqConstants.ES_SYNC_EXCHANGE,
                SearchMqConstants.ES_SYNC_DELETE_ROUTING_KEY,
                contentSyncMessage);
        mqMessageSendUtils.sendMqMessage(
                SearchMqConstants.MILVUS_SYNC_EXCHANGE,
                SearchMqConstants.MILVUS_SYNC_DELETE_ROUTING_KEY,
                contentSyncMessage);
    }
    /**
     * 清理并刷新博客缓存。
     * @return 执行结果
     */
    @Override
    public String flashCache() {
        redisService.deleteObject(redisService.keys(RedisConstants.CACHE_HOT_BLOG_KEY+"*"));
        redisService.deleteObject(redisService.keys(RedisConstants.CACHE_BLOG_KEY+"*"));
        redisService.deleteObject(redisService.keys(RedisConstants.CACHE_BLOG_TYPE_KEY+"*"));
        return null;
    }
    /**
     * 填充博客作者信息。
     *
     * @param blogVO 博客视图对象
     */
    private void queryBlogUser(BlogVO blogVO){
        if (blogVO == null) {
            return;
        }
        fillBlogUserNames(Collections.singletonList(blogVO));
    }
    /**
     * 判断当前用户是否点赞博客。
     *
     * @param blogVO 博客视图对象
     */
    private void isBlogLiked(BlogVO blogVO) {
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            blogVO.setIsLike(false);
            return;
        }
        Long userId = user.getId();
        LikeDTO likeDTO = new LikeDTO();
        likeDTO.setUserId(userId);
        likeDTO.setSourceId(blogVO.getId());
        likeDTO.setSourceType(GlobalBizTypeEnum.BLOG.getCode());
        Boolean isLike = remoteLikeService.isLike(likeDTO);
        blogVO.setIsLike(isLike);
    }

    /**
     * 判断当前用户是否收藏博客。
     *
     * @param blogVO 博客视图对象
     */
    private void isBlogStared(BlogVO blogVO) {
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            blogVO.setIsStared(false);
            return;
        }
        Long userId = user.getId();
        StarDTO starDTO = new StarDTO();
        starDTO.setUserId(userId);
        starDTO.setSourceId(blogVO.getId());
        starDTO.setSourceType(GlobalBizTypeEnum.BLOG.getCode());
        Boolean isStared = remoteStarService.isStar(starDTO);
        blogVO.setIsStared(isStared);
    }
    /**
     * 从缓存ZSet获取博客ID列表。
     *
     * @param key 缓存Key
     * @param size 数量
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
     * 将博客ID列表写入缓存ZSet。
     *
     * @param key 缓存Key
     * @param blogList 博客列表
     * @param timeout 过期时间
     * @param unit 时间单位
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
     * 构建博客列表ZSet前缀。
     *
     * @param key 缓存Key
     * @return 执行结果
     */
    private String buildBlogListZSetPrefix(String key) {
        return key + ":";
    }

    /**
     * 构建博客列表ZSet键。
     *
     * @param key 缓存Key
     * @return 执行结果
     */
    private String buildBlogListZSetKey(String key) {
        return buildBlogListZSetPrefix(key) + BLOG_LIST_ZSET_SLOT;
    }

    /**
     * 博客列表排序项。
     */
    private static class BlogListRankItem {
        private final Long id;
        private final Date scoreTime;

        private BlogListRankItem(Long id, Date scoreTime) {
            this.id = id;
            this.scoreTime = scoreTime;
        }

        /**
         * 获取排序项编号。
         *
         * @return 编号
         */
        private Long getId() {
            return id;
        }

        /**
         * 获取排序时间。
         * @return 排序时间
         */
        private Date getScoreTime() {
            return scoreTime;
        }
    }
    /**
     * 刷新单篇博客缓存。
     *
     * @param blogId 博客ID
     */
    private void flashRedisBlogCache(Long blogId) {
        redisService.deleteObject(RedisConstants.CACHE_BLOG_KEY+blogId);
    }
    /**
     * 刷新博客列表缓存。
     */
    private void flashRedisBlogListCache() {
        redisService.deleteObject(redisService.keys(RedisConstants.CACHE_BLOG_TYPE_KEY+"*"));
        redisService.deleteObject(redisService.keys(RedisConstants.CACHE_HOT_BLOG_KEY+"*"));
    }

    /**
     * 按标题关键字搜索博客。
     *
     * @param keyword 搜索关键词
     * @return 博客列表
     */
    @Override
    public List<BlogVO> searchBlogs(String keyword) {
        String trimmedKeyword = keyword == null ? null : keyword.trim();
        List<Blog> blogList = query()
                .select("id", "title", "status", "audit_status")
                .like(StrUtil.isNotBlank(trimmedKeyword), "title", trimmedKeyword)
                .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .list();
        return convertToBlogVOList(blogList);
    }
}
