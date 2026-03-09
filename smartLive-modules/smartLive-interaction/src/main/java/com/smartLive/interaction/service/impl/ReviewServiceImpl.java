package com.smartLive.interaction.service.impl;
import com.smartLive.common.core.constant.mq.AiAuditMqConstants;
import com.smartLive.common.core.constant.mq.SearchMqConstants;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.constant.PageConstants;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.AppLoginUser;
import com.smartLive.common.core.enums.AuditStatusEnum;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.RankRedisEnum;
import com.smartLive.common.core.enums.ReviewTypeEnum;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.rabbitmq.domain.AuditMessage;
import com.smartLive.common.rabbitmq.domain.ContentBatchSyncMessage;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.common.redis.util.CacheClient;
import com.smartLive.common.redis.util.RedisMultiCacheManager;
import com.smartLive.common.redis.util.ZSetIdManager;
import com.smartLive.interaction.domain.BO.AuditReviewBO;
import com.smartLive.interaction.domain.Like;
import com.smartLive.interaction.domain.Review;
import com.smartLive.interaction.domain.VO.ReviewVO;
import com.smartLive.interaction.api.DTO.LikeDTO;
import com.smartLive.interaction.domain.Star;
import com.smartLive.interaction.strategy.factory.ReviewStrategyFactory;
import com.smartLive.interaction.strategy.review.ReviewStrategy;
import org.springframework.beans.BeanUtils;
import com.smartLive.interaction.mapper.ReviewMapper;
import com.smartLive.interaction.service.ILikeService;
import com.smartLive.interaction.service.IReviewService;
import com.smartLive.interaction.service.IStarService;
import com.smartLive.interaction.strategy.factory.ResourceStrategyFactory;
import com.smartLive.interaction.strategy.resource.ResourceStrategy;
import com.smartLive.order.api.RemoteOrderService;
import com.smartLive.product.api.DTO.ProductDTO;
import com.smartLive.product.api.RemoteProductService;
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
 * 1. 坚持“单一职责原则”：本类仅处理评价业务의 CRUD、组装外部依赖（RPC）、以及发送数据到 Redis 缓存/消息队列。
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
    private RemoteProductService remoteProductService;

    @Autowired
    private MqMessageSendUtils mqMessageSendUtils;
    @Autowired
    private RedisMultiCacheManager redisMultiCacheManager;
    @Autowired
    private CacheClient cacheClient;
    private ResourceStrategyFactory resourceStrategyFactory;
    private ReviewStrategyFactory reviewStrategyFactory;

    @Autowired
    public ReviewServiceImpl(@Lazy ILikeService likeService, @Lazy IStarService starService, @Lazy ResourceStrategyFactory resourceStrategyFactory, @Lazy ReviewStrategyFactory reviewStrategyFactory) {
        this.likeService = likeService;
        this.starService = starService;
        this.resourceStrategyFactory = resourceStrategyFactory;
        this.reviewStrategyFactory = reviewStrategyFactory;
    }

    /**
     * 将Review实体转换为ReviewVO
     * @param review Review实体
     * @return ReviewVO对象
     */
    private ReviewVO convertToReviewVO(Review review) {
        if (review == null) {
            return null;
        }
        ReviewVO reviewVO = new ReviewVO();
        BeanUtils.copyProperties(review, reviewVO);
        return reviewVO;
    }

    /**
     * 将Review列表转换为ReviewVO列表
     * @param reviewList Review实体列表
     * @return ReviewVO列表
     */
    private List<ReviewVO> convertToReviewVOList(List<Review> reviewList) {
        if (CollUtil.isEmpty(reviewList)) {
            return new ArrayList<>();
        }
        return reviewList.stream()
                .map(this::convertToReviewVO)
                .collect(Collectors.toList());
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
    public List<ReviewVO> selectReviewList(Review review) {
        List<Review> reviews = reviewMapper.selectReviewList(review);
        List<ReviewVO> reviewVOList = convertToReviewVOList(reviews);
        // 根据资源类型进行分组
        Map<Integer, List<ReviewVO>> reviewMap = reviewVOList.stream()
                .collect(Collectors.groupingBy(ReviewVO::getSourceType));
        List<ReviewVO> result=new ArrayList<>();
        // 遍历资源类型进行分组
        reviewMap.forEach((sourceType, reviewVOS) -> {
            ReviewStrategy reviewStrategy = reviewStrategyFactory.getStrategy(sourceType);
            List<ReviewVO> re = reviewStrategy.setSourceName(reviewVOS);
            result.addAll(re);
        });
        return result;
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
        // 数据变更，必须清除详情缓存保证数据一致性
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
    public List<ReviewVO> listReview(Review review, Integer current, String sort) {
        ReviewTypeEnum reviewType = ReviewTypeEnum.getByCode(review.getSourceType());
        if (reviewType == null) {
            log.error("参数错误：未知的评价类型");
            return Collections.emptyList();
        }
        RankRedisEnum hotRankRedisEnum = RankRedisEnum.getByCategoryAndCode("REVIEW", reviewType.getCode());
        String reviewKeyPrefix = "";
        if (hotRankRedisEnum != null) {
            if ("latest".equalsIgnoreCase(sort)) {
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
                    .ne("status",3)
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

                    // 【核心补充】将从数据库里粗排兜底拉出来的数项 ID，推入热度待算队列，
                    // 交给底层的 XXL-Job 异步执行“基于衰减函数和各项互动数据”的精准重算。

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

        // 3. 第三步：转换为VO，组装动态的交互状态（是否点赞）与外部用户信息、商品信息
        List<ReviewVO> voList = convertToReviewVOList(list);
        queryReviewListIsLike(voList);
        queryReviewListUserMessage(voList);
        queryReviewListProductMessage(voList);

        // 挂载 AI 自动评价（如果存在）
        String key = RedisConstants.CACHE_AI_REVIEW_KEY + review.getSourceType() + ":" + review.getSourceId();
        String JsonStr = redisService.getCacheObject(key);
        if (JsonStr != null) {
            ReviewVO aiReview = JSON.parseObject(JsonStr, ReviewVO.class);
            voList.add(aiReview);
        }
        return voList;
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
                remoteOrderService.updateOrderReviewStatus(orderId, review.getId(), review.getCreateTime());
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
        mqMessageSendUtils.sendMqMessage( AiAuditMqConstants.AUDIT_DIRECT_EXCHANGE, AiAuditMqConstants.AUDIT_ROUTING_KEY, auditMessage);
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

            // 5. 评价被删，目标源的互动量减少，必须把它扔进算分队列，让定时任务给目标源【降温 / 降权】
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
    public List<ReviewVO> getReviewOfUser(Review review, Integer current) {
        if (review == null) {
            return Collections.emptyList();
        }
        Long userId = review.getUserId();
        if (userId == null) {
            AppLoginUser user = UserContextHolder.getUser();
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
        List<ReviewVO> voList = Collections.emptyList();
        int redisSourceCount = 0;

        if (reviewType != null) {
            Page<Long> idPage = zSetIdManager.pageIds(reviewType.getUserReviewKeyPrefix() + reviewType.getCode() + ":", userId, pageNo, pageSize);
            List<Long> sourceIdList = idPage.getRecords();
            redisSourceCount = CollUtil.isEmpty(sourceIdList) ? 0 : sourceIdList.size();
            if (CollUtil.isNotEmpty(sourceIdList)) {
                voList = getReviewListByIds(sourceIdList);
            }
        }
        if (redisSourceCount > 0 && voList.size() < redisSourceCount) {
            voList = Collections.emptyList();
        }
        if (CollUtil.isEmpty(voList)) {
            List<Review> dbList = query()
                    .eq("user_id", userId)
                    .eq(review.getSourceType() != null, "source_type", review.getSourceType())
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
            List<Review> pageList = dbList.subList(start, end);
            voList = convertToReviewVOList(pageList);
            queryReviewListIsLike(voList);
            queryReviewListUserMessage(voList);
            queryReviewListShopMessage(voList);
            queryReviewListProductMessage(voList);
        }
        return voList;
    }

    /**
     * 统计符合特定条件的评价数量
     * 优先级: Redis 独立计数器 -> review 表 COUNT
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
            // 2. 璁℃暟鍣ㄤ笉瀛樺湪锛屼粠 review 琛?COUNT 鏌ヨ骞跺洖鍐?Redis
            count = query()
                    .eq("source_type", review.getSourceType())
                    .eq("source_id", review.getSourceId())
                    .count().intValue();
            redisService.setCacheObject(reviewCountKey, count);
            return count;
        }
        // 閫氱敤鏉′欢鏌ヨ锛堝悗鍙扮鐞嗙瓑鍦烘櫙锛夛紝鐩存帴璧版暟鎹簱
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
    public List<ReviewVO> getReviewListByIds(List<Long> sourceIdList) {
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
        List<ReviewVO> voList = convertToReviewVOList(orderedList);
        queryReviewListUserMessage(voList);
        queryReviewListShopMessage(voList);
        queryReviewListProductMessage(voList);
        queryReviewListIsLike(voList);
        return voList;
    }

    /**
     * RPC 批量远程调用获取评价的店铺详情
     *
     * @param reviewList 待挂载店铺信息的评价列表
     */
    private void queryReviewListShopMessage(List<ReviewVO> reviewList) {
        if (CollUtil.isEmpty(reviewList)) {
            return;
        }
        List<Long> shopIds = reviewList.stream()
                .map(ReviewVO::getShopId)
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

        Map<Long, ShopDTO> shopMap = shopList.stream().collect(Collectors.toMap(
                ShopDTO::getId,
                Function.identity(),
                (v1, v2) -> v1
        ));
        reviewList.forEach(vo -> {
            if (vo.getShopId() != null && !vo.getShopId().toString().isEmpty()) {
                ShopDTO shopDTO = shopMap.get(vo.getShopId());
                if (shopDTO != null) {
                    vo.setShopLogo(shopDTO.getShopLogo());
                    vo.setShopName(shopDTO.getName());
                }
            }
        });
    }

    /**
     * RPC 批量远程调用获取评价关联的商品详情（标题、背景图、价格）
     *
     * @param reviewList 待挂载商品信息的评价列表
     */
    private void queryReviewListProductMessage(List<ReviewVO> reviewList) {
        if (CollUtil.isEmpty(reviewList)) {
            return;
        }
        // 绛涢€夊嚭 sourceType 涓哄晢鍝佺被鍨嬬殑璇勪环锛屾敹闆嗗叾 sourceId 浣滀负鍟嗗搧ID
        Integer productCode = GlobalBizTypeEnum.PRODUCT.getCode();
        List<Long> productIds = reviewList.stream()
                .filter(r -> productCode.equals(r.getSourceType()) && r.getSourceId() != null)
                .map(ReviewVO::getSourceId)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(productIds)) {
            return;
        }

        List<ProductDTO> productList = remoteProductService.getProductListByIds(productIds);
        if (CollUtil.isEmpty(productList)) {
            return;
        }

        Map<Long, ProductDTO> productMap = productList.stream().collect(Collectors.toMap(
                ProductDTO::getId,
                Function.identity(),
                (v1, v2) -> v1
        ));
        reviewList.forEach(vo -> {
            if (productCode.equals(vo.getSourceType()) && vo.getSourceId() != null) {
                ProductDTO productDTO = productMap.get(vo.getSourceId());
                if (productDTO != null) {
                    vo.setProductName(productDTO.getName());
                    vo.setProductCoverImg(productDTO.getCoverImg());
                    vo.setProductPrice(productDTO.getPrice());
                }
            }
        });
    }

    /**
     * RPC 批量远程调用获取发布评价的用户详情（昵称、头像）
     *
     * @param reviewList 待挂载用户信息系统的评价列表
     */
    private void queryReviewListUserMessage(List<ReviewVO> reviewList) {
        if (CollUtil.isEmpty(reviewList)) {
            return;
        }
        List<Long> userIds = reviewList.stream()
                .map(ReviewVO::getUserId)
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
        reviewList.forEach(vo -> {
            com.smartLive.user.api.domain.UserDTO user = userMap.get(vo.getUserId());
            if (user != null) {
                vo.setNickName(user.getNickName());
                vo.setUserIcon(user.getIcon());
            }
        });
    }
    /**
     * 批量查询当前登录用户对传入列表中各项评价的点赞状态
     *
     * @param reviewList 待校验点赞状态的评价列表
     */
    private void queryReviewListIsLike(List<ReviewVO> reviewList) {
        if (CollUtil.isEmpty(reviewList)) {
            return;
        }
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            reviewList.forEach(vo -> {
                if (vo != null) {
                    vo.setIsLike(false);
                }
            });
            return;
        }

        List<Long> reviewIds = reviewList.stream()
                .map(ReviewVO::getId)
                .collect(Collectors.toList());
        LikeDTO likeDTO = new LikeDTO();
        likeDTO.setUserId(user.getId());
        likeDTO.setSourceType(GlobalBizTypeEnum.REVIEW.getCode());
        Map<Long, Boolean> likeMap = likeService.isLikeBatch(likeDTO, reviewIds);

        reviewList.forEach(vo -> {
            if (vo != null) {
                vo.setIsLike(likeMap.getOrDefault(vo.getId(), false));
            }
        });
    }

    /**
     * 批量同步落库方法（被 XXL-JOB 调度任务回调使用）：
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
     * 鎵归噺鍚屾钀藉簱鏂规硶锛堣 XXL-JOB 璋冨害浠诲姟鍥炶皟浣跨敤锛?
     * 鍚屾鏁版嵁搴撲腑璇勪环鐨勫洖澶嶆暟閲忥紙瀛愯瘎璁烘暟锛夈€?
     *
     * @param updateMap K:璇勪环ID, V:鏈€鏂板洖澶嶆暟
     * @return 鏄惁澶勭悊鎴愬姛
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
     * 鎵归噺鍚屾钀藉簱鏂规硶锛堣 XXL-JOB 璋冨害浠诲姟鍥炶皟浣跨敤锛?
     * 鍚屾鏁版嵁搴撲腑璇勪环鐨勫叧娉?鏀惰棌鏁伴噺銆?
     *
     * @param updateMap K:璇勪环ID, V:鏈€鏂版敹钘忔暟
     * @return 鏄惁澶勭悊鎴愬姛
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
     * 鑾峰彇鍗曟潯璇勪环鐨勫綋鍓嶇偣璧炴€绘暟
     *
     * @param sourceId 璇勪环ID
     * @return 鐐硅禐鏁伴噺
     */
    @Override
    public Integer getReviewLikeCount(Long sourceId) {
        Review review = getById(sourceId);
        return review.getLiked();
    }

    /**
     * 鑱氬悎鏌ヨ鑾峰彇鍗曟潯璇勪环鐨勫畬鏁磋鎯?
     * 鍖呭惈瀵归槻缂撳瓨鍑荤┛锛堥€昏緫杩囨湡/浜掓枼閿侊級鐨勫簳灞傚皝瑁呰皟鐢紝骞惰仛鍚堣繙绔殑鍏宠仈涓氬姟鏁版嵁銆?
     *
     * @param id 璇勪环涓婚敭
     * @return 瀹屾暣鏁版嵁灏佽瀹炰綋
     */
    @Override
    public ReviewVO getReviewById(Long id) {
        Review review = cacheClient.queryWithLogicalExpireAndPassThrough(
                RedisConstants.CACHE_REVIEW_KEY,
                id,
                Review.class,
                this::getById,
                RedisConstants.CACHE_REVIEW_TTL,
                java.util.concurrent.TimeUnit.MINUTES
        );
        if (review == null) {
            return null;
        }
        ReviewVO vo = convertToReviewVO(review);

        Like like = new Like();
        like.setSourceType(GlobalBizTypeEnum.REVIEW.getCode());
        like.setSourceId(review.getId());
        vo.setIsLike(likeService.isLike(like));
        Star star = new Star();
        star.setSourceType(GlobalBizTypeEnum.REVIEW.getCode());
        star.setSourceId(review.getId());
        vo.setIsStared(starService.isStar(star));

        if (review.getShopId() != null && !review.getShopId().toString().isEmpty()) {
            ShopDTO shop = remoteShopService.getShopById(review.getShopId());
            if (shop != null) {
                vo.setShopName(shop.getName());
                vo.setShopLogo(shop.getImages());
            }
        }
        // 鎸傝浇鍟嗗搧淇℃伅锛堝晢鍝佹爣棰樸€佽儗鏅浘銆佷环鏍硷級
        Integer productCode = GlobalBizTypeEnum.PRODUCT.getCode();
        if (productCode.equals(review.getSourceType()) && review.getSourceId() != null) {
            ProductDTO productDTO = remoteProductService.getProductById(review.getSourceId());
            if (productDTO != null) {
                vo.setProductName(productDTO.getName());
                vo.setProductCoverImg(productDTO.getCoverImg());
                vo.setProductPrice(productDTO.getPrice());
            }
        }
        UserDTO userDTO = remoteAppUserService.queryUserById(review.getUserId());
        if (userDTO != null) {
            vo.setNickName(userDTO.getNickName());
            vo.setUserIcon(userDTO.getIcon());
        }
        return vo;
    }

    /**
     * 鑾峰彇鍗曟潯璇勪环鐨勫綋鍓嶆敹钘忔€绘暟
     *
     * @param sourceId 璇勪环ID
     * @return 鏀惰棌鏁伴噺
     */
    @Override
    public Integer getReviewStarCount(Long sourceId) {
        Review review = getById(sourceId);
        return review.getStared();
    }

    /**
     * 娓呯悊澶т竴缁熺殑鍗曟潯璇勪环璇︽儏缂撳瓨
     * 浠讳綍娑夊強鍒拌瘎浠峰唴瀹规垨浜掑姩鏁版嵁鏇存柊鐨勬搷浣滐紝鍧囬渶璋冪敤姝ゆ柟娉曚互纭繚鍚庣画璇诲彇鐨勬暟鎹柊椴滃害銆?
     *
     * @param reviewId 璇勪环涓婚敭
     */
    private void clearReviewCache(Long reviewId) {
        if (reviewId == null) {
            return;
        }
        redisService.deleteObject(RedisConstants.CACHE_REVIEW_KEY + reviewId);
    }

    /**
     * 鎵归噺娓呯悊璇勪环璇︽儏缂撳瓨
     * 涓昏鐢ㄤ簬鏁版嵁鎵归噺鍚屾钀藉簱鍚庣殑绾ц仈娓呯悊鍔ㄤ綔銆?
     *
     * @param reviewIds 璇勪环涓婚敭闆嗗悎
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
     * 淇濆瓨璇勪环 ID 鍒?Redis 鎺掕姒滀腑 (鍙戝竷鏃剁殑鏋佺畝鍗犱綅鐗?
     * 涓氬姟瑙ｈ€︼細涓轰簡淇濊瘉鎺ュ彛鍝嶅簲閫熷害锛屽彧缁欐柊璇勪环璧嬩簣鏃堕棿鎴充綔涓哄垵濮嬪垎銆?
     * 绮剧‘鐨勫熀浜庝簰鍔ㄩ噺涓庢椂闂磋“鍑忕殑鐑害鍒嗘暟閲嶇畻锛屽畬鍏ㄧЩ浜よ嚦 SyncDataServiceImpl 寮傛澶勭悊銆?
     *
     * @param reviewType 璇勪环鐩爣婧愮被鍨?
     * @param review 璇勪环瀹炰綋
     */
    private void saveReviewRankToRedis(ReviewTypeEnum reviewType, Review review) {
        if (reviewType == null || review == null || review.getId() == null || review.getSourceId() == null) {
            return;
        }
        long scoreTime = review.getCreateTime() == null ? System.currentTimeMillis() : review.getCreateTime().getTime();
        RankRedisEnum hotRankRedisEnum = RankRedisEnum.getByCategoryAndCode("REVIEW", reviewType.getCode());
        String hotRankKey = hotRankRedisEnum.getHotRankKeyPrefix() + review.getSourceId();
        String newRankKey = hotRankRedisEnum.getNewRankKeyPrefix() + review.getSourceId();

        // 1. 銆愭渶鏂版銆戯細缁濆鍑嗙‘銆傜洿鎺ュ瓨鍏ュ彂甯冩椂闂存埑銆?
        redisService.setCacheZSet(newRankKey, review.getId().toString(), scoreTime);

        // 2. 銆愮儹闂ㄦ銆戯細璧嬩簣涓€涓瀬楂樼殑鍒濆鍒嗭紙褰撳墠鏃堕棿鎴筹級锛屼繚璇佺敤鎴峰垰鍙戝畬璇勪环鑳界灛闂存帓鍦ㄦ棣栥€?
        // 鐪熷疄鐨勨€滈噸鍔涜“鍑忊€濈簿缁嗗寲绠楀垎锛屼氦鐢?XXL-JOB 瀹氭椂浠诲姟绋嶅悗鏉ユ礂鐗屻€?
        redisService.setCacheZSet(hotRankKey, review.getId().toString(), (double) scoreTime);
    }

        /**
     * 全量发布评价数据到向量库（仅 Milvus）。
     *
     * @return 发布结果
     */
    @Override
    public String allPublish() {
        int page = PageConstants.PAGE_NUMBER;
        int pageSize = PageConstants.ES_PAGE_SIZE;
        while (true) {
            List<Review> reviews = query()
                    .page(new Page<>(page, pageSize))
                    .getRecords();
            if (CollUtil.isEmpty(reviews)) {
                break;
            }
            int finalPage = page;
            executorService.submit(() -> {
                log.info("线程{}，开始发布第{}页评价数据", Thread.currentThread().getName(), finalPage);
                sendReviewMilvusBatchMessage(reviews);
                log.info("线程{}，发布第{}页完成，数量={}", Thread.currentThread().getName(), finalPage, reviews.size());
            });
            page++;
        }
        return "数据发布完成";
    }

    /**
     * 按 ID 发布评价数据到向量库（仅 Milvus）。
     *
     * @param ids 评价 ID 数组
     * @return 发布结果
     */
    @Override
    public String publish(String[] ids) {
        if (ids == null || ids.length == 0) {
            return "参数为空";
        }
        List<Long> idList = Arrays.stream(ids)
                .map(Long::valueOf)
                .collect(Collectors.toList());

        executorService.submit(() -> {
            log.info("线程{}，开始发布指定评价：{}", Thread.currentThread().getName(), idList);
            List<Review> reviews = query().in("id", idList).list();
            if (CollUtil.isNotEmpty(reviews)) {
                sendReviewMilvusBatchMessage(reviews);
            }
        });
        return "发布成功";
    }

    /**
     * 批量发送评价同步消息（仅同步到 Milvus 向量库）。
     *
     * @param reviews 评价列表
     */
    private void sendReviewMilvusBatchMessage(List<?> reviews) {
        if (CollUtil.isEmpty(reviews)) {
            return;
        }
        ContentBatchSyncMessage request = new ContentBatchSyncMessage();
        request.setIndexName("review_index");
        request.setData(reviews);
        request.setType(GlobalBizTypeEnum.REVIEW.getCode());

        mqMessageSendUtils.sendMqMessage(
                SearchMqConstants.MILVUS_SYNC_EXCHANGE,
                SearchMqConstants.MILVUS_SYNC_BATCH_INSERT_ROUTING_KEY,
                request);
    }

    /**
     * 运营后台处理评价审核状态。
     *
     * @param id 评价主键
     * @param status 审核状态
     * @param reason 拒绝原因
     * @return 是否处理成功
     */
    @Override
    public Boolean updateReviewStatus(Long id, Integer status, String reason) {
        // 拒绝时记录拒绝原因，通过时清空拒绝原因
        String rejectReason = AuditStatusEnum.isRejected(status) ? reason : null;
        boolean update = update(new UpdateWrapper<Review>()
                .set("status", status)
                .set("reject_reason", rejectReason)
                .eq("id", id));
        if (update) {
            clearReviewCache(id);
        }
        // 若审核被拒绝，需要同步将该评价从排行榜中移除并触发所属主体降分逻辑
        if(update && AuditStatusEnum.isRejected(status)){
            Review review = getById(id);
            ReviewTypeEnum reviewType = ReviewTypeEnum.getByCode(review.getSourceType());
            RankRedisEnum hotRankRedisEnum = RankRedisEnum.getByCategoryAndCode("REVIEW", reviewType.getCode());
            String reviewKeyPrefix = hotRankRedisEnum.getHotRankKeyPrefix()+ review.getSourceId();
            String reviewNewRankKeyPrefix = hotRankRedisEnum.getNewRankKeyPrefix()+ review.getSourceId();
            String reviewCountKeyPrefix = reviewType.getReviewCountKeyPrefix()+ review.getSourceId();
            String reviewSyncKey = reviewType.getReviewSyncKey();

            // 灏嗚繚瑙勮瘎浠蜂粠鎺掕姒滀腑鍓旈櫎
            redisService.removeCacheZSetObject(reviewKeyPrefix, review.getId().toString());
            redisService.removeCacheZSetObject(reviewNewRankKeyPrefix, review.getId().toString());
            // 3. 确保 Redis 计数器已初始化，再递减
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
     * @return true=已评价 false=未评价
     */
    @Override
    public Boolean isReview(Review review) {
        if (review == null || review.getSourceType() == null || review.getSourceId() == null) {
            return false;
        }
        Long userId = review.getUserId();
        if (userId == null) {
            AppLoginUser user = UserContextHolder.getUser();
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

        // 缓存未命中时查库兜底，状态 2 (已删除) 的无效数据不计入其中
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
     * 鏋勫缓鐢ㄤ簬鏍囪鐢ㄦ埛鈥滃凡璇勪环鐩爣婧愨€濊冻杩瑰巻鍙茬殑 Redis ZSet Key
     *
     * @param reviewType 璇勪环绫诲瀷鏋氫妇
     * @param userId 瑙﹀彂琛屼负鐨勭敤鎴?ID
     * @return Redis Key
     */
    private String userReviewKey(ReviewTypeEnum reviewType, Long userId) {
        if (reviewType == null || userId == null) {
            return null;
        }
        return reviewType.getUserReviewKeyPrefix() + reviewType.getCode() + ":" + userId;
    }

    /**
     * 褰撳彂鐢熺墿鐞嗗垹闄ゆ垨杩濊涓嬫灦鎿嶄綔鏃惰皟鐢ㄧ殑琛ュ伩娓呯悊閫昏緫銆?
     * 鑻ヨ鐢ㄦ埛鍦ㄨ鐩爣婧愪笅锛屽凡娌℃湁浠讳綍鏈夋晥鐨勮瘎浠疯褰曪紝鍒欏悓姝ユ摝闄?Redis 涓粬鐨勨€滃凡璇勪环鈥濊冻杩广€?
     *
     * @param reviewType 璇勪环绫诲瀷鏋氫妇
     * @param review 琚搷浣滅殑璇勪环瀹炰綋
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
