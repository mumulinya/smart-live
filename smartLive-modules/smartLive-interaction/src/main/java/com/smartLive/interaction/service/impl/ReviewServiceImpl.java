package com.smartLive.interaction.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.enums.AuditStatusEnum;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.ReviewTypeEnum;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.rabbitmq.domain.AuditMessage;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.common.redis.util.CacheClient;
import com.smartLive.common.redis.util.RedisMultiCacheManager;
import com.smartLive.common.redis.util.ZSetIdManager;
import com.smartLive.interaction.domain.BO.AuditReviewBO;
import com.smartLive.interaction.domain.Like;
import com.smartLive.interaction.domain.Review;
import com.smartLive.interaction.api.DTO.LikeDTO;
import com.smartLive.interaction.domain.DTO.StarDTO;
import com.smartLive.interaction.domain.Star;
import com.smartLive.interaction.mapper.ReviewMapper;
import com.smartLive.interaction.service.ILikeService;
import com.smartLive.interaction.service.IReviewService;
import com.smartLive.interaction.service.IStarService;
import com.smartLive.interaction.strategy.factory.ResourceStrategyFactory;
import com.smartLive.interaction.strategy.resource.ResourceStrategy;
import com.smartLive.order.api.RemoteOrderService;
import com.smartLive.shop.api.DTO.ShopDTO;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartLive.user.api.domain.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评价ervice业务层处理
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Service
@Slf4j
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements IReviewService {
    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private RemoteAppUserService remoteAppUserService;

    @Autowired
    private RemoteShopService remoteShopService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private ZSetIdManager zSetIdManager;

    private ILikeService likeService;
    private IStarService starService;
    @Autowired
    private RemoteOrderService remoteOrderService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisMultiCacheManager redisMultiCacheManager;
    @Autowired
    private CacheClient cacheClient;
    private ResourceStrategyFactory resourceStrategyFactory;

    @Autowired
    public ReviewServiceImpl(@Lazy ILikeService likeService, @Lazy IStarService starService, @Lazy ResourceStrategyFactory resourceStrategyFactory) {
        this.likeService = likeService;
        this.starService = starService;
        this.resourceStrategyFactory = resourceStrategyFactory;
    }
    /**
     * 查询评论
     *
     * @param id 评论主键
     * @return 评论
     */
    @Override
    public Review selectReviewById(Long id) {
        return reviewMapper.selectReviewById(id);
    }

    /**
     * 查询评论列表
     *
     * @param review 评论
     * @return 评论
     */
    @Override
    public List<Review> selectReviewList(Review review) {
        return reviewMapper.selectReviewList(review);
    }

    /**
     * 新增评论
     *
     * @param review 评论
     * @return 结果
     */
    @Override
    public int insertReview(Review review) {
        review.setCreateTime(DateUtils.getNowDate());
        return reviewMapper.insertReview(review);
    }

    /**
     * 修改评论
     *
     * @param review 评论
     * @return 结果
     */
    @Override
    public int updateReview(Review review) {
        review.setUpdateTime(DateUtils.getNowDate());
        int i = reviewMapper.updateReview(review);
        if (i > 0&&review.getStatus() == 0) {
            //发送审核信息
            sendAuditMessage(review);
        }
        if (i > 0) {
            clearReviewCache(review.getId());
        }
        return  i;
    }

    /**
     * 批量删除评论
     *
     * @param ids 需要删除的评论主键
     * @return 结果
     */
    @Override
    public int deleteReviewByIds(Long[] ids) {
        int rows = reviewMapper.deleteReviewByIds(ids);
        if (rows > 0 && ids != null && ids.length > 0) {
            clearReviewCacheBatch(Arrays.asList(ids));
        }
        return rows;
    }

    /**
     * 删除评论信息
     *
     * @param id 评论主键
     * @return 结果
     */
    @Override
    public int deleteReviewById(Long id) {
        int rows = reviewMapper.deleteReviewById(id);
        if (rows > 0) {
            clearReviewCache(id);
        }
        return rows;
    }

    /**
     * 获取评价列表
     *
     * @param review
     * @return
     */
    @Override
    public List<Review> listReview(Review review, Integer current) {
        // Redis 前缀
        ReviewTypeEnum reviewType = ReviewTypeEnum.getByCode(review.getSourceType());
        if (reviewType == null) {
            log.error("参数错误");
            return Collections.emptyList();
        }
        String reviewKeyPrefix = reviewType.getReviewKeyPrefix();

        // 1. 使用 QueryRedisSourceIdsTool 获取分页 ID 列表 (ZSet 分页)
        Page<Long> longPage = zSetIdManager.pageIds(reviewKeyPrefix, review.getSourceId(), current, SystemConstants.MAX_PAGE_SIZE);
        List<Long> reviewIdList = longPage.getRecords();

        if (CollUtil.isEmpty(reviewIdList)) {
            // 降级：如果 Redis ZSet 为空，则查询数据库获取 Review ID
            log.info("Redis ZSet empty, querying DB for Review IDs");
             List<Review> dbList = query()
                    .eq("source_id", review.getSourceId())
                    .ne("status", 2)
                    .eq("source_type", review.getSourceType())
                    .orderByDesc("liked")
                    .list();

            if (CollUtil.isNotEmpty(dbList)) {
                // 保存到 Redis ZSet
                saveReviewListToRedis(reviewKeyPrefix + review.getSourceId(), dbList);
                // 简单的内存分页用于降级
                int start = (current - 1) * SystemConstants.MAX_PAGE_SIZE; // Note: using MAX_PAGE_SIZE as per original logic? Or DEFAULT?
                // The original code used MAX_PAGE_SIZE in queryRedisIdPage, but DEFAULT_PAGE_SIZE in DB fallback subList??
                // Reviewing original code: usage was inconsistent.
                // Assuming SystemConstants.MAX_PAGE_SIZE is the intended page size for reviews?
                // Step 683 showed: queryRedisIdPage(..., MAX_PAGE_SIZE) and DB subList(..., DEFAULT_PAGE_SIZE).
                // This implies a bug in original code or intended mismatch.
                // Let's stick to MAX_PAGE_SIZE for consistency with Redis call.
                int pageSize = SystemConstants.MAX_PAGE_SIZE;
                 if (dbList.size() > start) {
                    dbList = dbList.subList(start, Math.min(start + pageSize, dbList.size()));
                    reviewIdList = dbList.stream().map(Review::getId).collect(Collectors.toList());
                } else {
                     reviewIdList = Collections.emptyList();
                }
            }
        }

        if (CollUtil.isEmpty(reviewIdList)) {
            return Collections.emptyList();
        }

        // 2. 使用 RedisBatchCacheUtil 批量获取 Review 对象
        List<Review> list = redisMultiCacheManager.queryBatchWithCache(
                RedisConstants.CACHE_REVIEW_KEY, // Review 对象缓存前缀
                reviewIdList,                    // 要获取的 ID
                Review.class,                    // 目标类
                (missingIds) -> {                // 对象数据库降级
                    return query()
                            .in("id", missingIds)
                            .list();
                },
                Review::getId,                   // ID 提取器
                RedisConstants.CACHE_REVIEW_TTL, // 过期时间
                java.util.concurrent.TimeUnit.MINUTES // 单位
        );

        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }

        // 3. 批量获取辅助信息 (User, Like, Star)
        Set<Long> userIds = list.stream().map(Review::getUserId).collect(Collectors.toSet());
        List<Long> reviewIds = list.stream().map(Review::getId).collect(Collectors.toList());

        // 3.1 批量获取用户
        Map<Long, UserDTO> userMap = new HashMap<>();
        if (CollUtil.isNotEmpty(userIds)) {
            try {
                // Assuming remoteAppUserService has a batch method. If not, we might need to loop or add one.
                // Based on previous context, queryUserById is single. checking for batch...
                // RemoteAppUserService has getUserList(List<Long> userIdList)
                List<UserDTO> users = remoteAppUserService.getUserList(new ArrayList<>(userIds));
                if (CollUtil.isNotEmpty(users)) {
                    userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
                }
            } catch (Exception e) {
                log.error("Batch fetch users failed", e);
            }
        }

        // 3.2 批量获取点赞状态
        Map<Long, Boolean> likeMap = new HashMap<>();
        try {
            LikeDTO likeDTO = new LikeDTO();
            likeDTO.setSourceType(GlobalBizTypeEnum.REVIEW.getCode());
             // 注意: RemoteLikeService.getIsLikeBatch 需要实现检查
             // 我们已经验证该接口存在
            likeMap = likeService.isLikeBatch(likeDTO, reviewIds);
        } catch (Exception e) {
             log.error("Batch fetch like status failed", e);
        }

        // 3.3 批量获取收藏状态
        Map<Long, Boolean> starMap = new HashMap<>();
        try {
            StarDTO starDTO = new StarDTO();
            starDTO.setSourceType(GlobalBizTypeEnum.REVIEW.getCode());
            starMap = starService.isStarBatch(starDTO, reviewIds);
        } catch (Exception e) {
            log.error("Batch fetch star status failed", e);
        }

        // 4. 组装数据
        for (Review c : list) {
            // 用户信息
            UserDTO user = userMap.get(c.getUserId());
            if (user != null) {
                c.setNickName(user.getNickName());
                c.setUserIcon(user.getIcon());
            }

            // 点赞状态
            c.setIsLike(likeMap.getOrDefault(c.getId(), false));

            // 收藏状态
            c.setIsStared(starMap.getOrDefault(c.getId(), false));
        }

        // 5. AI 评论 (保持现有逻辑)
        String key = RedisConstants.CACHE_AI_COMMENT_KEY + review.getSourceType() + ":" + review.getSourceId();
        String JsonStr = redisService.getCacheObject(key);
        if (JsonStr != null) {
            Review reviewDTO = JSON.parseObject(JsonStr, Review.class);
            list.add(reviewDTO);
        }
        return list;
    }

    /**
     * 新增评论
     *
     * @param review
     * @return
     */
    @Override
    @Transactional
    public Integer addReview(Review review) {
        review.setCreateTime(DateUtils.getNowDate());
        int i = reviewMapper.insertReview(review);
        if (i > 0) {
            //发送审核信息
            sendAuditMessage(review);
            Long orderId = review.getOrderId();
            //更新订单评价状态，设置为已评价
            if (orderId != null) {
                remoteOrderService.updateOrderReviewStatus(orderId);
            }
            ReviewTypeEnum reviewType = ReviewTypeEnum.getByCode(review.getSourceType());
            if (reviewType == null) {
                log.error("reviewType is null, sourceType={}", review.getSourceType());
                return i;
            }
            String reviewKeyPrefix = reviewType.getReviewKeyPrefix()+ review.getSourceId();
            String reviewCountKeyPrefix = reviewType.getReviewCountKeyPrefix()+ review.getSourceId();
            String reviewDirtyKeyPrefix = reviewType.getReviewDirtyKeyPrefix();
            String userReviewKey = userReviewKey(reviewType, review.getUserId());
            //保存评价信息
            redisService.setCacheZSet(reviewKeyPrefix, review.getId().toString(), System.currentTimeMillis());
            if (userReviewKey != null) {
                redisService.setCacheZSet(userReviewKey, review.getSourceId().toString(), System.currentTimeMillis());
            }
            //记录评价数量
            redisService.incrementCacheValue(reviewCountKeyPrefix);
            //记录脏数据
            redisService.setCacheSet(reviewDirtyKeyPrefix, Collections.singleton(review.getSourceId().toString()));
        }
        return i;
    }
    /**
     * 发送审核消息
     * @param review
     */
    private void sendAuditMessage(Review review) {
        ResourceStrategy resourceType = resourceStrategyFactory.getStrategy(review.getSourceType());
        HashMap<String, String> content = resourceType.getResourceContentById(review.getSourceId());
        AuditReviewBO auditReviewBO=new AuditReviewBO();
        BeanUtil.copyProperties(review, auditReviewBO);
        auditReviewBO.setTargetTitle(content.get("title"));
        auditReviewBO.setTargetImages(content.get("images"));
        AuditMessage auditMessage = AuditMessage.builder()
                .bizId(review.getId())
                .bizType(GlobalBizTypeEnum.REVIEW.getCode())
                .submitterId(review.getUserId())
                .auditContent(BeanUtil.beanToMap(auditReviewBO))
                .createTime(review.getCreateTime())
                .build();
        MqMessageSendUtils.sendMqMessage(rabbitTemplate, MqConstants.AUDIT_EXCHANGE_NAME,MqConstants.AUDIT_ROUTING_KEY, auditMessage);
    }
    /**
     * 删除评论
     *
     * @param review
     * @return
     */
    @Override
    @Transactional
    public Boolean deleteReview(Review review) {
        Review dbReview = getById(review.getId());
        if (dbReview == null) {
            return false;
        }
        boolean i = removeById(dbReview.getId());
        if (i) {
            clearReviewCache(dbReview.getId());
            ReviewTypeEnum reviewType = ReviewTypeEnum.getByCode(dbReview.getSourceType());
            if (reviewType == null) {
                return true;
            }
            String reviewKeyPrefix = reviewType.getReviewKeyPrefix()+ dbReview.getSourceId();
            String reviewCountKeyPrefix = reviewType.getReviewCountKeyPrefix()+ dbReview.getSourceId();
            String reviewDirtyKeyPrefix = reviewType.getReviewDirtyKeyPrefix();
            //删除评论信息
            redisService.removeCacheZSetObject(reviewKeyPrefix, dbReview.getId().toString());
            //记录评论数量
            redisService.decrementCacheValue(reviewCountKeyPrefix);
            //记录脏数据
            redisService.setCacheSet(reviewDirtyKeyPrefix, Collections.singleton(dbReview.getSourceId().toString()));
            evictUserReviewSourceIfNeeded(reviewType, dbReview);
        }
        return i;
    }

    /**
     * 获取用户的评价
     *
     * @param current
     * @return
     */
    @Override
    public List<Review> getReviewOfUser(Review review,Integer current) {
        if (review == null) {
            return Collections.emptyList();
        }
        Long userId = review.getUserId();
        if (userId == null) {
            com.smartLive.common.core.domain.UserDTO user = UserContextHolder.getUser();
            if (user != null) {
                userId = user.getId();
                review.setUserId(userId);
            }
        }
        if (userId == null) {
            return Collections.emptyList();
        }

        int pageNo = current == null || current < 1 ? 1 : current;
        int pageSize = SystemConstants.MAX_PAGE_SIZE;
        ReviewTypeEnum reviewType = ReviewTypeEnum.getByCode(review.getSourceType());
        List<Review> list = Collections.emptyList();
        int redisSourceCount = 0;

        if (reviewType != null) {
            Page<Long> idPage = zSetIdManager.pageIds(reviewType.getUserReviewKeyPrefix() + reviewType.getCode() + ":", userId, pageNo, pageSize);
            List<Long> sourceIdList = idPage.getRecords();
            redisSourceCount = CollUtil.isEmpty(sourceIdList) ? 0 : sourceIdList.size();
            if (CollUtil.isNotEmpty(sourceIdList)) {
//                list = queryLatestUserReviewBySourceIds(userId, review.getSourceType(), review.getStatus(), sourceIdList);
                list=getReviewListByIds(sourceIdList);
            }
        }
        if (redisSourceCount > 0 && list.size() < redisSourceCount) {
            list = Collections.emptyList();
        }

        if (CollUtil.isEmpty(list)) {
            List<Review> dbList = query()
                    .eq("user_id", userId)
                    .eq(review.getSourceType() != null, "source_type", review.getSourceType())
                    .eq(review.getStatus() != null, "status", review.getStatus())
                    .orderByDesc("create_time")
                    .list();

            if (CollUtil.isEmpty(dbList)) {
                return Collections.emptyList();
            }

            if (reviewType != null) {
                //保存用户评价源
                zSetIdManager.saveToZSet(userReviewKey(reviewType, userId),dbList, Review::getId, Review::getCreateTime);
            }
            //截取数据
            int start = (pageNo - 1) * pageSize;
            if (start >= dbList.size()) {
                return Collections.emptyList();
            }
            int end = Math.min(start + pageSize, dbList.size());
            list=dbList.subList(start, end);
        }
        enrichUserReviewList(list, userId);
        return list;
    }

    /**
     * 获取评价列表的用户信息
     *
     * @param
     * @return
     */
    private void enrichUserReviewList(List<Review> list, Long userId) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        UserDTO owner = remoteAppUserService.queryUserById(userId);
        for (Review r : list) {
            if (owner != null) {
                r.setNickName(owner.getNickName());
                r.setUserIcon(owner.getIcon());
            }
            Long shopId = r.getShopId() != null ? r.getShopId() : r.getSourceId();
            if (shopId != null) {
                ShopDTO shop = remoteShopService.getShopById(shopId);
                if (shop != null) {
                    r.setSourceName(shop.getName());
                    r.setShopImages(shop.getImages());
                }
            }
        }
    }

    /**
     * 获取用户发表的评论数
     *
     * @param review
     * @return
     */
    @Override
    public Integer getReviewCount(Review review) {
        return query()
                .eq(review.getSourceType() != null, "source_type", review.getSourceType())
                .eq(review.getSourceId() != null, "source_id", review.getSourceId())
                .eq(review.getUserId() != null, "user_id", review.getUserId())
                .count().intValue();
    }

    /**
     * 获取评论总数
     *
     * @return
     */
    @Override
    public Integer getReviewTotal() {
        return query().count().intValue();
    }

    /**
     * 根据评论id列表获取评论列表
     *
     * @param sourceIdList
     * @return
     */
    @Override
    public List<Review> getReviewListByIds(List<Long> sourceIdList) {
        if (CollUtil.isEmpty(sourceIdList)) {
            return Collections.emptyList();
        }

        List<Review> list = redisMultiCacheManager.queryBatchWithCache(
                RedisConstants.CACHE_REVIEW_KEY,
                sourceIdList,
                Review.class,
                missingIds -> query().in("id", missingIds).list(),
                Review::getId,
                RedisConstants.CACHE_REVIEW_TTL,
                java.util.concurrent.TimeUnit.MINUTES
        );
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }

        Map<Long, Review> reviewMap = list.stream()
                .filter(review -> review != null && review.getId() != null)
                .collect(Collectors.toMap(Review::getId, review -> review, (v1, v2) -> v1));
        List<Review> orderedList = new ArrayList<>(sourceIdList.size());
        for (Long id : sourceIdList) {
            Review review = reviewMap.get(id);
            if (review != null) {
                orderedList.add(review);
            }
        }
        if (CollUtil.isEmpty(orderedList)) {
            return Collections.emptyList();
        }

        List<Long> userIds = orderedList.stream()
                .map(Review::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(userIds)) {
            try {
                List<UserDTO> userList = remoteAppUserService.getUserList(userIds);
                if (CollUtil.isNotEmpty(userList)) {
                    Map<Long, UserDTO> userMap = userList.stream().collect(Collectors.toMap(
                            UserDTO::getId,
                            user -> user,
                            (v1, v2) -> v1
                    ));
                    orderedList.forEach(review -> {
                        UserDTO user = userMap.get(review.getUserId());
                        if (user != null) {
                            review.setNickName(user.getNickName());
                            review.setUserIcon(user.getIcon());
                        }
                    });
                }
            } catch (Exception e) {
                log.error("Batch query review user info failed, reviewIds={}", sourceIdList, e);
            }
        }
        return orderedList;
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
        clearReviewCacheBatch(updateMap.keySet());
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
        clearReviewCacheBatch(updateMap.keySet());
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
        clearReviewCacheBatch(updateMap.keySet());
        return true;
    }

    /**
     * 获取评论点赞数
     *
     * @param sourceId
     * @return
     */
    @Override
    public Integer getReviewLikeCount(Long sourceId) {
        Review review = getById(sourceId);
        return review.getLiked();
    }

    /**
     * 根据id获取评论详情
     *
     * @param id
     * @return
     */
    @Override
    public Review getReviewById(Long id) {
        Review review = cacheClient.queryWithLogicalExpireAndPassThrough(
                RedisConstants.CACHE_REVIEW_KEY,
                id,
                Review.class,
                this::getById,
                RedisConstants.CACHE_REVIEW_TTL,
                java.util.concurrent.TimeUnit.MINUTES
        );
        if (review != null) {
            //判断是否点赞
            Like like = new Like();
            like.setSourceType(GlobalBizTypeEnum.REVIEW.getCode());
            like.setSourceId(review.getId());
            review.setIsLike(likeService.isLike(like));
            //判断是否收藏
            Star star = new Star();
            star.setSourceType(GlobalBizTypeEnum.REVIEW.getCode());
            star.setSourceId(review.getId());
            review.setIsStared(starService.isStar(star));
            ShopDTO shop = remoteShopService.getShopById(review.getShopId());
            if(shop!=null){
                review.setSourceName(shop.getName());
                review.setShopImages(shop.getImages());
            }
            UserDTO userDTO = remoteAppUserService.queryUserById(review.getUserId());
            if(userDTO!=null){
                review.setNickName(userDTO.getNickName());
                review.setUserIcon(userDTO.getIcon());
            }
        }
        return review;
    }

    /**
     * 获取评价收藏数
     *
     * @param sourceId
     * @return
     */
    @Override
    public Integer getReviewStarCount(Long sourceId) {
        Review review = getById(sourceId);
        return review.getStared();
    }

    private void clearReviewCache(Long reviewId) {
        if (reviewId == null) {
            return;
        }
        redisService.deleteObject(RedisConstants.CACHE_REVIEW_KEY + reviewId);
    }

    private void clearReviewCacheBatch(Collection<Long> reviewIds) {
        if (CollUtil.isEmpty(reviewIds)) {
            return;
        }
        List<String> keys = reviewIds.stream()
                .filter(Objects::nonNull)
                .map(id -> RedisConstants.CACHE_REVIEW_KEY + id)
                .toList();
        if (CollUtil.isNotEmpty(keys)) {
            redisService.deleteObject(keys);
        }
    }

    /**
     * 保存点赞用户列表到Redis
     *
     * @param key
      * @param reviewList
     */
    private void saveReviewListToRedis(String key,List<Review> reviewList) {
        log.info("保存点赞用户列表到Redis{}",reviewList);
        if (CollUtil.isNotEmpty(reviewList)) {
            zSetIdManager.saveToZSet(key, reviewList, Review::getId, Review::getCreateTime);
        }
    }

    /**
     * 更新评价状态
     * @param id 评价ID
     * @param status 状态
     * @return
     */
    @Override
    public Boolean updateReviewStatus(Long id, Integer status) {
        boolean update = update(new UpdateWrapper<Review>()
                .set("status", status)
                .eq("id", id));
        if (update) {
            clearReviewCache(id);
        }
        if(update&&status== AuditStatusEnum.REJECT.getCode()){
            Review review = getById(id);
            ReviewTypeEnum reviewType = ReviewTypeEnum.getByCode(review.getSourceType());
            String reviewKeyPrefix = reviewType.getReviewKeyPrefix()+ review.getSourceId();
            String reviewCountKeyPrefix = reviewType.getReviewCountKeyPrefix()+ review.getSourceId();
            String reviewDirtyKeyPrefix = reviewType.getReviewDirtyKeyPrefix();
            //删除评论信息
            redisService.removeCacheZSetObject(reviewKeyPrefix, review.getId().toString());
            //记录评论数量
            redisService.decrementCacheValue(reviewCountKeyPrefix);
            //记录脏数据
            redisService.setCacheSet(reviewDirtyKeyPrefix, Collections.singleton(review.getSourceId().toString()));
            evictUserReviewSourceIfNeeded(reviewType, review);
        }
        return update;

    }

    @Override
    public Boolean isReview(Review review) {
        if (review == null || review.getSourceType() == null || review.getSourceId() == null) {
            return false;
        }
        Long userId = review.getUserId();
        if (userId == null) {
            com.smartLive.common.core.domain.UserDTO user = UserContextHolder.getUser();
            if (user != null) {
                userId = user.getId();
            }
        }
        if (userId == null) {
            return false;
        }
        ReviewTypeEnum reviewType = ReviewTypeEnum.getByCode(review.getSourceType());
        if (reviewType == null) {
            return false;
        }
        String userReviewKey = userReviewKey(reviewType, userId);
        if (userReviewKey == null) {
            return false;
        }
        if (redisService.getCacheZSetScore(userReviewKey, review.getSourceId().toString()) != null) {
            return true;
        }

        long count = query()
                .eq("user_id", userId)
                .eq("source_type", review.getSourceType())
                .eq("source_id", review.getSourceId())
                .ne("status", 2)
                .count();
        if (count > 0) {
            redisService.setCacheZSet(userReviewKey, review.getSourceId().toString(), System.currentTimeMillis());
            return true;
        }
        return false;
    }

    private String userReviewKey(ReviewTypeEnum reviewType, Long userId) {
        if (reviewType == null || userId == null) {
            return null;
        }
        return reviewType.getUserReviewKeyPrefix() + reviewType.getCode() + ":" + userId;
    }

    private void evictUserReviewSourceIfNeeded(ReviewTypeEnum reviewType, Review review) {
        if (reviewType == null || review == null || review.getUserId() == null || review.getSourceId() == null) {
            return;
        }
        long remains = query()
                .eq("user_id", review.getUserId())
                .eq("source_type", review.getSourceType())
                .eq("source_id", review.getSourceId())
                .ne("status", 2)
                .count();
        if (remains <= 0) {
            redisService.removeCacheZSetObject(userReviewKey(reviewType, review.getUserId()), review.getSourceId().toString());
        }
    }
}
