package com.smartLive.interaction.service.impl;
import com.smartLive.common.core.constant.mq.AiAuditMqConstants;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.enums.AuditStatusEnum;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.RankRedisEnum;
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
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 评价服务核心实现类
 *
 * 架构说明：
 * 1. 坚持“单一职责原则”：本类仅处理评价业务的 CRUD、组装外部依赖（RPC）、以及发送数据到 Redis 缓存/消息队列。
 * 2. 动静分离设计：评价详情使用统一的 CACHE_REVIEW_KEY 进行存储，排行榜 ID 列表按业务隔离存储在 ZSet 中。
 * 3. 异步计算解耦：所有复杂的动态热度分计算均移交至 SyncDataServiceImpl 的 XXL-JOB 定时任务处理。
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
    @Autowired
    private ExecutorService executorService;

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
     * 根据 ID 查询单条评价信息（直接查库，无缓存包装）
     *
     * @param id 评价主键
     * @return 评价实体
     */
    @Override
    public Review selectReviewById(Long id) {
        return reviewMapper.selectReviewById(id);
    }

    /**
     * 根据条件查询评价列表（通常用于后台管理系统的全量/条件搜索）
     *
     * @param review 查询条件
     * @return 评价集合
     */
    @Override
    public List<Review> selectReviewList(Review review) {
        return reviewMapper.selectReviewList(review);
    }

    /**
     * 底层新增评价数据（不包含复杂缓存与队列流转逻辑）
     *
     * @param review 评价实体
     * @return 影响行数
     */
    @Override
    public int insertReview(Review review) {
        review.setCreateTime(DateUtils.getNowDate());
        return reviewMapper.insertReview(review);
    }

    /**
     * 更新评价信息，并同步清理详情缓存、触发重新审核机制
     *
     * @param review 评价实体
     * @return 影响行数
     */
    @Override
    public int updateReview(Review review) {
        review.setUpdateTime(DateUtils.getNowDate());
        int i = reviewMapper.updateReview(review);
        // 如果更新后状态变为待审核(0)，重新触发审核流
        if (i > 0 && review.getStatus() == 0) {
            sendAuditMessage(review);
        }
        // 数据变更，必须清除统一的详情缓存保证数据一致性
        if (i > 0) {
            clearReviewCache(review.getId());
        }
        return i;
    }

    /**
     * 批量物理删除评价，并同步清理相关缓存
     *
     * @param ids 评价主键数组
     * @return 影响行数
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
     * 物理删除单条评价，并清理相关缓存
     *
     * @param id 评价主键
     * @return 影响行数
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
     * 【核心读链路】分页获取前台展示的评价列表
     * 采用大厂标准的 "ID List (ZSet) + 详情缓存 (Cache)" 的两步走架构，极大减轻数据库压力。
     *
     * @param review  查询条件（包含 sourceType 和 sourceId）
     * @param current 当前页码
     * @return 组装好的评价视图列表
     */
    @Override
    public List<Review> listReview(Review review, Integer current) {
        ReviewTypeEnum reviewType = ReviewTypeEnum.getByCode(review.getSourceType());
        if (reviewType == null) {
            log.error("参数错误：未知的评价类型");
            return Collections.emptyList();
        }
        RankRedisEnum hotRankRedisEnum = RankRedisEnum.getByCategoryAndCode("REVIEW", reviewType.getCode());
        String reviewKeyPrefix = "";
        if (hotRankRedisEnum != null) {
            if ("latest".equalsIgnoreCase(review.getSort())) {
                reviewKeyPrefix = hotRankRedisEnum.getNewRankKeyPrefix();
            } else {
                reviewKeyPrefix = hotRankRedisEnum.getHotRankKeyPrefix();
            }
        }

        // 1. 第一步：从 ZSet 排行榜中极速获取排好序的评价 ID 列表
        Page<Long> longPage = zSetIdManager.pageIds(reviewKeyPrefix, review.getSourceId(), current, SystemConstants.MAX_PAGE_SIZE);
        List<Long> reviewIdList = longPage.getRecords();

        // 兜底逻辑：如果缓存击穿（ZSet 中没有数据），去 MySQL 查询并重建 ZSet 缓存
        if (CollUtil.isEmpty(reviewIdList)) {
            log.info("Redis ZSet empty, querying DB for Review IDs");
            List<Review> dbList = query()
                    .eq("source_id", review.getSourceId())
                    .ne("status", 2)
                    .eq("source_type", review.getSourceType())
                    .orderByDesc("liked")
                    .orderByDesc("create_time")
                    .list();

            if (CollUtil.isNotEmpty(dbList)) {
                final List<Review> finalDbList = dbList;
                executorService.execute(() -> {
                    String hotRankKey = hotRankRedisEnum.getHotRankKeyPrefix() + review.getSourceId();
                    String newRankKey = hotRankRedisEnum.getNewRankKeyPrefix() + review.getSourceId();
                    zSetIdManager.saveToZSet(hotRankKey, finalDbList, Review::getId, Review::getCreateTime);
                    zSetIdManager.saveToZSet(newRankKey, finalDbList, Review::getId, Review::getCreateTime);

                    // 【核心补充】将从数据库里粗排兜底拉出来的数据 ID，推入热度待算队列，
                    // 交给底层的 XXL-Job 异步执行“基于衰减函数和各项互动数据”的精准重算！

                    if (hotRankRedisEnum.getCalcQueueKey() != null) {
                        redisService.setCacheSet(hotRankRedisEnum.getCalcQueueKey(),finalDbList.stream().map(r -> String.valueOf(r.getId())).collect(Collectors.toSet()));
                    }
                });

                int start = (current - 1) * SystemConstants.MAX_PAGE_SIZE;
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

        // 2. 第二步：根据拿到的 ID 列表，去统一的详情缓存中批量拉取数据
        List<Review> list = redisMultiCacheManager.queryBatchWithCache(
                RedisConstants.CACHE_REVIEW_KEY, // 大一统的统一前缀
                reviewIdList,
                Review.class,
                (missingIds) -> {
                    // 如果有部分数据在缓存中过期了，回源查库补齐
                    return query().in("id", missingIds).list();
                },
                Review::getId,
                RedisConstants.CACHE_REVIEW_TTL,
                java.util.concurrent.TimeUnit.MINUTES
        );

        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }

        // 3. 第三步：组装动态的交互状态（是否点赞）与外部用户信息
        queryReviewListIsLike(list);
        queryReviewListUserMessage(list);

        // 挂载 AI 自动评价（如果存在）
        String key = RedisConstants.CACHE_AI_REVIEW_KEY + review.getSourceType() + ":" + review.getSourceId();
        String JsonStr = redisService.getCacheObject(key);
        if (JsonStr != null) {
            Review reviewDTO = JSON.parseObject(JsonStr, Review.class);
            list.add(reviewDTO);
        }
        return list;
    }

    /**
     * 【核心写链路】用户前端发布新增评价
     * 采用极轻量的落库和发消息操作，复杂的排序计算全权交由异步队列处理，保证前端响应速度。
     *
     * @param review 评价实体
     * @return 影响行数
     */
    @Override
    @Transactional
    public Integer addReview(Review review) {
        review.setCreateTime(DateUtils.getNowDate());
        int i = reviewMapper.insertReview(review);
        if (i > 0) {
            // 1. 发送给审核中心
            sendAuditMessage(review);

            // 2. 更新订单维度的评价状态
            Long orderId = review.getOrderId();
            if (orderId != null) {
                remoteOrderService.updateOrderReviewStatus(orderId);
            }

            ReviewTypeEnum reviewType = ReviewTypeEnum.getByCode(review.getSourceType());
            if (reviewType == null) {
                log.error("reviewType is null, sourceType={}", review.getSourceType());
                return i;
            }

            // 3. 将新评价置入排行榜展示（仅时间占位，不进行复杂计算）
            saveReviewRankToRedis(reviewType, review);

            // 记录该用户的评价发布历史
            String userReviewKey = userReviewKey(reviewType, review.getUserId());
            if (userReviewKey != null) {
                redisService.setCacheZSet(userReviewKey, review.getSourceId().toString(), System.currentTimeMillis());
            }

            String reviewCountKeyPrefix = reviewType.getReviewCountKeyPrefix()+ review.getSourceId();
            String reviewSyncKey = reviewType.getReviewSyncKey();

            // 4. 确保 Redis 计数器已初始化，再自增目标主体的评价总数
            getReviewCount(review);
            redisService.incrementCacheValue(reviewCountKeyPrefix);

            // 5. 【双轨制：同步轨】将发生互动的数据源 ID 推入待同步队列
            redisService.setCacheSet(reviewSyncKey, Collections.singleton(review.getSourceId().toString()));

            // 6. 【双轨制：算分轨】将新增的评价 ID 推入待算分队列，触发 XXL-JOB 稍后基于衰减算法重排榜单
            RankRedisEnum hotRankRedisEnum = RankRedisEnum.getByCategoryAndCode("REVIEW", reviewType.getCode());
            if (hotRankRedisEnum != null) {
                redisService.setCacheSet(hotRankRedisEnum.getCalcQueueKey(), review.getId().toString());
            }
        }
        return i;
    }

    /**
     * 构建并发送 MQ 消息，触发异步风控拦截与机器审核逻辑
     *
     * @param review 待审核的评价实体
     */
    private void sendAuditMessage(Review review) {
        ResourceStrategy resourceType = resourceStrategyFactory.getStrategy(review.getSourceType());
        HashMap<String, String> content = resourceType.getResourceContentById(review.getSourceId());
        AuditReviewBO auditReviewBO = new AuditReviewBO();
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
        MqMessageSendUtils.sendMqMessage(rabbitTemplate, AiAuditMqConstants.AUDIT_EXCHANGE_NAME, AiAuditMqConstants.AUDIT_ROUTING_KEY, auditMessage);
    }

    /**
     * 用户前端逻辑删除自己的评价
     * 必须清理详情缓存，并将操作同步至排行榜和双轨制计算队列中。
     *
     * @param review 待删除的评价实体（包含主键 ID）
     * @return 是否删除成功
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
            // 1. 清理大一统详情缓存
            clearReviewCache(dbReview.getId());

            ReviewTypeEnum reviewType = ReviewTypeEnum.getByCode(dbReview.getSourceType());
            if (reviewType == null) {
                return true;
            }

            RankRedisEnum hotRankRedisEnum = RankRedisEnum.getByCategoryAndCode("REVIEW", reviewType.getCode());
            String reviewKeyPrefix = hotRankRedisEnum.getHotRankKeyPrefix()+ dbReview.getSourceId();
            String reviewNewRankKeyPrefix = hotRankRedisEnum.getNewRankKeyPrefix()+ dbReview.getSourceId();
            String reviewCountKeyPrefix = reviewType.getReviewCountKeyPrefix()+ dbReview.getSourceId();
            String reviewSyncKey = reviewType.getReviewSyncKey();

            // 2. 从各项展示排行榜中彻底抹除该 ID
            redisService.removeCacheZSetObject(reviewKeyPrefix, dbReview.getId().toString());
            redisService.removeCacheZSetObject(reviewNewRankKeyPrefix, dbReview.getId().toString());

            // 3. 确保 Redis 计数器已初始化，再递减目标主体的评价总数
            getReviewCount(dbReview);
            redisService.decrementCacheValue(reviewCountKeyPrefix);

            // 4. 将目标源 ID 放入待落库同步队列
            redisService.setCacheSet(reviewSyncKey, Collections.singleton(dbReview.getSourceId().toString()));

            // 5. 评价被删，目标源的互动量减少，必须把它扔进算分队列，让定时任务给目标源【降火/降权】
            if (hotRankRedisEnum != null) {
                redisService.setCacheSet(hotRankRedisEnum.getCalcQueueKey(), dbReview.getId().toString());
            }

            evictUserReviewSourceIfNeeded(reviewType, dbReview);
        }
        return i;
    }

    /**
     * 获取指定用户（当前登录用户）发布过的评价列表，支持分页
     *
     * @param review  查询条件封装
     * @param current 当前页码
     * @return 用户的评价历史列表
     */
    @Override
    public List<Review> getReviewOfUser(Review review, Integer current) {
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
                list = getReviewListByIds(sourceIdList);
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
                    .orderByDesc("liked")
                    .orderByDesc("create_time")
                    .list();

            if (CollUtil.isEmpty(dbList)) {
                return Collections.emptyList();
            }

            if (reviewType != null) {
                zSetIdManager.saveToZSet(userReviewKey(reviewType, userId), dbList, Review::getId, Review::getCreateTime);
            }
            int start = (pageNo - 1) * pageSize;
            if (start >= dbList.size()) {
                return Collections.emptyList();
            }
            int end = Math.min(start + pageSize, dbList.size());
            list = dbList.subList(start, end);
            queryReviewListIsLike(list);
            queryReviewListUserMessage(list);
        }
        queryReviewListShopMessage(list);
        return list;
    }

    /**
     * 统计符合特定条件的评价数量
     * 优先级: Redis 独立计数器 → review 表 COUNT
     *
     * @param review 查询条件
     * @return 统计总数
     */
    @Override
    public Integer getReviewCount(Review review) {
        ReviewTypeEnum reviewType = ReviewTypeEnum.getByCode(review.getSourceType());
        // 只有同时指定了 sourceType 和 sourceId 时才走 Redis 计数器
        if (reviewType != null && review.getSourceId() != null) {
            String reviewCountKey = reviewType.getReviewCountKeyPrefix() + review.getSourceId();
            // 1. 从 Redis 独立计数器读取
            Integer count = redisService.getCacheObject(reviewCountKey);
            if (count != null) {
                return count;
            }
            // 2. 计数器不存在，从 review 表 COUNT 查询并回写 Redis
            count = query()
                    .eq("source_type", review.getSourceType())
                    .eq("source_id", review.getSourceId())
                    .count().intValue();
            redisService.setCacheObject(reviewCountKey, count);
            return count;
        }
        // 通用条件查询（后台管理等场景），直接走数据库
        return query()
                .eq(review.getSourceType() != null, "source_type", review.getSourceType())
                .eq(review.getSourceId() != null, "source_id", review.getSourceId())
                .eq(review.getUserId() != null, "user_id", review.getUserId())
                .count().intValue();
    }

    /**
     * 获取全站系统的评价总数
     *
     * @return 总数量
     */
    @Override
    public Integer getReviewTotal() {
        return query().count().intValue();
    }

    /**
     * 根据内部 ID 列表，批量从 Redis 详情池中拉取评价实体（核心通用查询方法）
     *
     * @param sourceIdList 评价 ID 列表
     * @return 对应的评价实体列表
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
        queryReviewListUserMessage(orderedList);
        queryReviewListIsLike(orderedList);

        return orderedList;
    }

    /**
     * RPC 批量远程调用获取评价的店铺详情
     *
     * @param reviewList 待挂载店铺信息的评价列表
     */
    private void queryReviewListShopMessage(List<Review> reviewList) {
        if (CollUtil.isEmpty(reviewList)) {
            return;
        }
        List<Long> shopIds = reviewList.stream()
                .map(Review::getShopId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(shopIds)) {
            return;
        }

        List<ShopDTO> userList = remoteShopService.getShopList(shopIds);
        if (CollUtil.isEmpty(userList)) {
            return;
        }

        Map<Long, ShopDTO> userMap = userList.stream().collect(Collectors.toMap(
                ShopDTO::getId,
                Function.identity(),
                (v1, v2) -> v1
        ));
        reviewList.forEach(review -> {
            ShopDTO shopDTO = userMap.get(review.getUserId());
            if (shopDTO != null) {
                review.setShopLogo(shopDTO.getShopLogo());
                review.setShopName((shopDTO.getName()));
            }
        });
    }

    /**
     * RPC 批量远程调用获取发布评价的用户详情（昵称、头像）
     *
     * @param reviewList 待挂载用户信息的评价列表
     */
    private void queryReviewListUserMessage(List<Review> reviewList) {
        if (CollUtil.isEmpty(reviewList)) {
            return;
        }
        List<Long> userIds = reviewList.stream()
                .map(Review::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(userIds)) {
            return;
        }

        List<UserDTO> userList = remoteAppUserService.getUserList(userIds);
        if (CollUtil.isEmpty(userList)) {
            return;
        }

        Map<Long, UserDTO> userMap = userList.stream().collect(Collectors.toMap(
                com.smartLive.user.api.domain.UserDTO::getId,
                Function.identity(),
                (v1, v2) -> v1
        ));
        reviewList.forEach(review -> {
            com.smartLive.user.api.domain.UserDTO user = userMap.get(review.getUserId());
            if (user != null) {
                review.setNickName(user.getNickName());
                review.setUserIcon(user.getIcon());
            }
        });
    }
    /**
     * 批量查询当前登录用户对传入列表中各项评价的点赞状态
     *
     * @param reviewList 待校验点赞状态的评价列表
     */
    private void queryReviewListIsLike(List<Review> reviewList) {
        if (CollUtil.isEmpty(reviewList)) {
            return;
        }
        com.smartLive.common.core.domain.UserDTO user = UserContextHolder.getUser();
        if (user == null) {
            reviewList.forEach(review -> {
                if (review != null) {
                    review.setIsLike(false);
                }
            });
            return;
        }

        List<Long> reviewIds = reviewList.stream()
                .map(Review::getId)
                .collect(Collectors.toList());
        LikeDTO likeDTO = new LikeDTO();
        likeDTO.setUserId(user.getId());
        likeDTO.setSourceType(GlobalBizTypeEnum.REVIEW.getCode());
        Map<Long, Boolean> likeMap = likeService.isLikeBatch(likeDTO, reviewIds);

        reviewList.forEach(review -> {
            if (review != null) {
                review.setIsLike(likeMap.getOrDefault(review.getId(), false));
            }
        });
    }

    /**
     * 批量同步落库方法（被 XXL-JOB 调度任务回调使用）
     * 批量更新点赞数后，必须清除对应的详情缓存，避免出现前台展示脏读。
     *
     * @param updateMap K:评价ID, V:最新点赞数
     * @return 是否处理成功
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
        clearReviewCacheBatch(updateMap.keySet());
        return true;
    }

    /**
     * 批量同步落库方法（被 XXL-JOB 调度任务回调使用）
     * 同步数据库中评价的回复数量（子评论数）。
     *
     * @param updateMap K:评价ID, V:最新回复数
     * @return 是否处理成功
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
        clearReviewCacheBatch(updateMap.keySet());
        return true;
    }

    /**
     * 批量同步落库方法（被 XXL-JOB 调度任务回调使用）
     * 同步数据库中评价的关注/收藏数量。
     *
     * @param updateMap K:评价ID, V:最新收藏数
     * @return 是否处理成功
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
        clearReviewCacheBatch(updateMap.keySet());
        return true;
    }

    /**
     * 获取单条评价的当前点赞总数
     *
     * @param sourceId 评价ID
     * @return 点赞数量
     */
    @Override
    public Integer getReviewLikeCount(Long sourceId) {
        Review review = getById(sourceId);
        return review.getLiked();
    }

    /**
     * 聚合查询获取单条评价的完整详情
     * 包含对防缓存击穿（逻辑过期/互斥锁）的底层封装调用，并聚合远端的关联业务数据。
     *
     * @param id 评价主键
     * @return 完整数据封装实体
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
            Like like = new Like();
            like.setSourceType(GlobalBizTypeEnum.REVIEW.getCode());
            like.setSourceId(review.getId());
            review.setIsLike(likeService.isLike(like));
            Star star = new Star();
            star.setSourceType(GlobalBizTypeEnum.REVIEW.getCode());
            star.setSourceId(review.getId());
            review.setIsStared(starService.isStar(star));

            ShopDTO shop = remoteShopService.getShopById(review.getShopId());
            if(shop!=null){
                review.setShopName(shop.getName());
                review.setShopLogo(shop.getImages());
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
     * 获取单条评价的当前收藏总数
     *
     * @param sourceId 评价ID
     * @return 收藏数量
     */
    @Override
    public Integer getReviewStarCount(Long sourceId) {
        Review review = getById(sourceId);
        return review.getStared();
    }

    /**
     * 清理大一统的单条评价详情缓存
     * 任何涉及到评价内容或互动数据更新的操作，均需调用此方法以确保后续读取的数据新鲜度。
     *
     * @param reviewId 评价主键
     */
    private void clearReviewCache(Long reviewId) {
        if (reviewId == null) {
            return;
        }
        redisService.deleteObject(RedisConstants.CACHE_REVIEW_KEY + reviewId);
    }

    /**
     * 批量清理评价详情缓存
     * 主要用于数据批量同步落库后的级联清理动作。
     *
     * @param reviewIds 评价主键集合
     */
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
     * 保存评价 ID 到 Redis 排行榜中 (发布时的极简占位版)
     * 业务解耦：为了保证接口响应速度，只给新评价赋予时间戳作为初始分。
     * 精确的基于互动量与时间衰减的热度分数重算，完全移交至 SyncDataServiceImpl 异步处理。
     *
     * @param reviewType 评价目标源类型
     * @param review 评价实体
     */
    private void saveReviewRankToRedis(ReviewTypeEnum reviewType, Review review) {
        if (reviewType == null || review == null || review.getId() == null || review.getSourceId() == null) {
            return;
        }
        long scoreTime = review.getCreateTime() == null ? System.currentTimeMillis() : review.getCreateTime().getTime();
        RankRedisEnum hotRankRedisEnum = RankRedisEnum.getByCategoryAndCode("REVIEW", reviewType.getCode());
        String hotRankKey = hotRankRedisEnum.getHotRankKeyPrefix() + review.getSourceId();
        String newRankKey = hotRankRedisEnum.getNewRankKeyPrefix() + review.getSourceId();

        // 1. 【最新榜】：绝对准确。直接存入发布时间戳。
        redisService.setCacheZSet(newRankKey, review.getId().toString(), scoreTime);

        // 2. 【热门榜】：赋予一个极高的初始分（当前时间戳），保证用户刚发完评价能瞬间排在榜首。
        // 真实的“重力衰减”精细化算分，交由 XXL-JOB 定时任务稍后来洗牌。
        redisService.setCacheZSet(hotRankKey, review.getId().toString(), (double) scoreTime);
    }

    /**
     * 运营后台处理评价审核状态
     *
     * @param id 评价主键
     * @param status 更新后的审核状态
     * @return 是否处理成功
     */
    @Override
    public Boolean updateReviewStatus(Long id, Integer status) {
        boolean update = update(new UpdateWrapper<Review>()
                .set("status", status)
                .eq("id", id));
        if (update) {
            clearReviewCache(id);
        }
        // 如果审核被拒绝，必须将其从前端的展示榜单中彻底移除，并触发依赖其主体的降分逻辑
        if(update && status == AuditStatusEnum.REJECT.getCode()){
            Review review = getById(id);
            ReviewTypeEnum reviewType = ReviewTypeEnum.getByCode(review.getSourceType());
            RankRedisEnum hotRankRedisEnum = RankRedisEnum.getByCategoryAndCode("REVIEW", reviewType.getCode());
            String reviewKeyPrefix = hotRankRedisEnum.getHotRankKeyPrefix()+ review.getSourceId();
            String reviewNewRankKeyPrefix = hotRankRedisEnum.getNewRankKeyPrefix()+ review.getSourceId();
            String reviewCountKeyPrefix = reviewType.getReviewCountKeyPrefix()+ review.getSourceId();
            String reviewSyncKey = reviewType.getReviewSyncKey();

            // 将违规评价从排行榜中剔除
            redisService.removeCacheZSetObject(reviewKeyPrefix, review.getId().toString());
            redisService.removeCacheZSetObject(reviewNewRankKeyPrefix, review.getId().toString());
            // 确保 Redis 计数器已初始化，再递减
            getReviewCount(review);
            redisService.decrementCacheValue(reviewCountKeyPrefix);
            redisService.setCacheSet(reviewSyncKey, Collections.singleton(review.getSourceId().toString()));
            // 通知定时任务扣减所属主体的热度分
            if (hotRankRedisEnum != null) {
                redisService.setCacheSet(hotRankRedisEnum.getCalcQueueKey(), review.getId().toString());
            }

            evictUserReviewSourceIfNeeded(reviewType, review);
        }
        return update;
    }

    /**
     * 判断指定用户是否已经对某个具体业务源（如某个店铺）做出了评价
     * 常用作发布前的防刷校验，或前台 UI “去评价”按钮的状态控制。
     *
     * @param review 封装了目标源和用户ID的参数对象
     * @return true=已评价, false=未评价
     */
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
        // 优先拦截：尝试从用户的发评足迹 ZSet 中命中
        if (redisService.getCacheZSetScore(userReviewKey, review.getSourceId().toString()) != null) {
            return true;
        }

        // 缓存未命中时查库兜底，状态 2 (已删除)的无效数据不计入其中
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

    /**
     * 构建用于标记用户“已评价目标源”足迹历史的 Redis ZSet Key
     *
     * @param reviewType 评价类型枚举
     * @param userId 触发行为的用户 ID
     * @return Redis Key
     */
    private String userReviewKey(ReviewTypeEnum reviewType, Long userId) {
        if (reviewType == null || userId == null) {
            return null;
        }
        return reviewType.getUserReviewKeyPrefix() + reviewType.getCode() + ":" + userId;
    }

    /**
     * 当发生物理删除或违规下架操作时调用的补偿清理逻辑。
     * 若该用户在该目标源下，已没有任何有效的评价记录，则同步擦除 Redis 中他的“已评价”足迹。
     *
     * @param reviewType 评价类型枚举
     * @param review 被操作的评价实体
     */
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

    /**
     * 接收并缓存 AI 总结生成的评价
     */
    @Override
    public Boolean saveAiCreateReview(List<Review> reviews) {
        if (reviews.size() == 0) {
            throw new RuntimeException("评价列表不能为空");
        }
        redisService.deleteObject(redisService.keys(RedisConstants.CACHE_AI_REVIEW_KEY + "*"));
        reviews.forEach(reviewDTO -> {
            String key = RedisConstants.CACHE_AI_REVIEW_KEY + reviewDTO.getSourceType() + ":" + reviewDTO.getSourceId();
            redisService.setCacheObject(key, com.alibaba.fastjson.JSON.toJSONString(reviewDTO));
        });
        return true;
    }
}