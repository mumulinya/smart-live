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
import com.smartLive.common.core.enums.ContentStatusEnum;
import com.smartLive.common.core.enums.common.AuditStatusEnum;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.common.RankRedisEnum;
import com.smartLive.common.core.enums.interaction.ReviewTypeEnum;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.security.utils.SecurityUtils;
import com.smartLive.common.rabbitmq.domain.AuditMessage;
import com.smartLive.common.rabbitmq.domain.ContentBatchSyncMessage;
import com.smartLive.common.rabbitmq.domain.ContentSyncMessage;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.common.redis.util.CacheClient;
import com.smartLive.common.redis.util.RedisMultiCacheManager;
import com.smartLive.common.redis.util.ZSetIdManager;
import com.smartLive.interaction.domain.BO.AuditReviewBO;
import com.smartLive.interaction.domain.Like;
import com.smartLive.interaction.domain.Review;
import com.smartLive.interaction.domain.VO.BadReviewVO;
import com.smartLive.interaction.domain.VO.ReviewVO;
import com.smartLive.interaction.domain.VO.ShopReviewAnalysisVO;
import com.smartLive.interaction.domain.VO.ShopReviewSuggestVO;
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
import com.smartLive.system.api.RemoteUserService;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartLive.user.api.domain.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 评价服务核心实现层
 * 实现了针对全站各类资源（店铺、商品、代金券等）的图文评价管理。
 *
 * 核心技术栈：
 * 1. 动静分离逻辑：详情存 Cache，热算关系存 ZSet Rank。
 * 2. 异步双轨计算：实时落库与异步重排并行，通过 XXL-JOB 实现衰减因子的精准热度算分。
 * 3. AI 向量检索集成：同步评价数据至 Milvus 向量库，支撑语义维度的智能内容检索。
 * 4. 自动化审核流：集成 AI 内容分析，实现评价发布的即时合规性检测。
 *
 * @author smartLive
 * @date 2026-03-11
 */
@Service
@Slf4j
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements IReviewService {
    private static final java.time.format.DateTimeFormatter REVIEW_ANALYSIS_TIME_FORMATTER = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private RemoteAppUserService remoteAppUserService;
    @Autowired
    private RemoteShopService remoteShopService;
    @Autowired
    private RemoteUserService remoteUserService;
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
    public ReviewVO selectReviewById(Long id) {
        ReviewVO reviewVO = convertToReviewVO(reviewMapper.selectReviewById(id));
        queryReviewUserInfo(reviewVO);
        return reviewVO;
    }
    /**
     * 查询评价用户信息
     *
     * @param reviewVO 评价信息
     */
    private void queryReviewUserInfo(ReviewVO reviewVO) {
        if (reviewVO == null) {
            return;
        }
        UserDTO userDTO = remoteAppUserService.getUserInfoById(reviewVO.getUserId());
        reviewVO.setNickName(userDTO.getNickName());
        reviewVO.setUserIcon(userDTO.getIcon());
    }

    /**
     * 根据条件查询评价列表（通常用于后台管理系统的全量/条件搜索）
     *
     * @param review 查询条件
     * @return 评价集合
     */
    @Override
    public List<ReviewVO> selectReviewList(Review review) {
        if (review == null) {
            review = new Review();
        }
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId != null && !SecurityUtils.isAdmin(currentUserId)) {
            List<Long> shopIds = remoteUserService.getShopIdsByUserId(currentUserId);
            if (CollUtil.isEmpty(shopIds)) {
                return Collections.emptyList();
            }
            review.setShopIds(shopIds);
        }

        List<Review> reviews = reviewMapper.selectReviewList(review);
        List<ReviewVO> reviewVOList = convertToReviewVOList(reviews);
        // 根据资源类型进行分组
        Map<Integer, List<ReviewVO>> reviewMap = reviewVOList.stream()
                .collect(Collectors.groupingBy(ReviewVO::getSourceType));
        List<ReviewVO> result = new ArrayList<>();
        // 遍历资源类型进行分组
        reviewMap.forEach((sourceType, reviewVOS) -> {
            ReviewStrategy reviewStrategy = reviewStrategyFactory.getStrategy(sourceType);
            List<ReviewVO> re = reviewStrategy.setSourceName(reviewVOS);
            result.addAll(re);
        });
        queryReviewListUserMessage(result);
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
        int i = reviewMapper.insertReview(review);
        if (i > 0) {
            // 同步写入向量库
            publish(new String[]{review.getId().toString()});
        }
        return i;
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
        review.setAuditStatus(AuditStatusEnum.WAITING.getCode());
        review.setRejectReason(null);
        int i = reviewMapper.updateReview(review);
        // 评价被修改后统一回到待审核态，并重新触发审核流
        if (i > 0 && !Objects.equals(review.getStatus(), ContentStatusEnum.DRAFT.getCode())) {
            sendAuditMessage(review);
        }
        // 数据变更，必须清除详情缓存保证数据一致性
        if (i > 0) {
            clearReviewCache(review.getId());
            // 同步更新向量库(如果已经存在会删除后重新插入)
            publish(new String[]{review.getId().toString()});
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
            // 同步删除向量库数据
            for (Long id : ids) {
                executorService.submit(() -> {
                    ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
                    contentSyncMessage.setId(id);
                    contentSyncMessage.setIndexName("review_index");
                    contentSyncMessage.setType(GlobalBizTypeEnum.REVIEW.getCode());
                    mqMessageSendUtils.sendMqMessage(
                            SearchMqConstants.MILVUS_SYNC_EXCHANGE,
                            SearchMqConstants.MILVUS_SYNC_DELETE_ROUTING_KEY,
                            contentSyncMessage);
                });
            }
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
            // 同步删除向量库数据
            ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
            contentSyncMessage.setId(id);
            contentSyncMessage.setIndexName("review_index");
            contentSyncMessage.setType(GlobalBizTypeEnum.REVIEW.getCode());
            mqMessageSendUtils.sendMqMessage(
                    SearchMqConstants.MILVUS_SYNC_EXCHANGE,
                    SearchMqConstants.MILVUS_SYNC_DELETE_ROUTING_KEY,
                    contentSyncMessage);
        }
        return rows;
    }

    /**
     * 【核心读链路】分页获取前台展示的评价列表
     * 采用大厂标准的 "ID List (ZSet) + 详情缓存 (Cache)" 的两步走架构，极大减轻数据库压力。
     *
     * @param review  查询条件（包含 sourceType 和 sourceId）
     * @param current 当前页码
     * @param sort 排序方式
     * @return 组装好的评价视图列表
     */
    @Override
    public List<ReviewVO> listReview(Review review, Integer current, String sort) {
        ReviewTypeEnum reviewType = ReviewTypeEnum.getByCode(review.getSourceType());
        if (reviewType == null) {
            log.error("invalid review type");
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
        if (CollUtil.isEmpty(reviewIdList)&&current == 1) {
            log.info("Redis ZSet empty, querying DB for Review IDs");
            List<Review> dbList = query()
                    .eq("source_id", review.getSourceId())
                    .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
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

        // 2. 第二步：根据拿到的 ID 列表，去根据id列表获取列表的方法批量获取评价详情
        List<ReviewVO> list = getReviewListByIds(reviewIdList);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }

        // 挂载 AI 自动评价（如果存在）
        String key = RedisConstants.CACHE_AI_REVIEW_KEY + review.getSourceType() + ":" + review.getSourceId();
        String JsonStr = redisService.getCacheObject(key);
        if (JsonStr != null) {
            ReviewVO aiReview = JSON.parseObject(JsonStr, ReviewVO.class);
            list.add(aiReview);
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
            if (Objects.equals(review.getStatus(), ContentStatusEnum.DRAFT.getCode())) {
                return i;
            }
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

            // 7. 同步新增的数据到向量库
            publish(new String[]{review.getId().toString()});
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
            if (Objects.equals(dbReview.getStatus(), ContentStatusEnum.PUBLISHED.getCode())) {
                // 3. ensure counters and rank only for published review
                getReviewCount(dbReview);
                redisService.decrementCacheValue(reviewCountKeyPrefix);
                redisService.setCacheSet(reviewSyncKey, Collections.singleton(dbReview.getSourceId().toString()));
                if (hotRankRedisEnum != null) {
                    redisService.setCacheSet(hotRankRedisEnum.getCalcQueueKey(), dbReview.getId().toString());
                }
            }

            // 6. 同步删除向量库数据
            ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
            contentSyncMessage.setId(dbReview.getId());
            contentSyncMessage.setIndexName("review_index");
            contentSyncMessage.setType(GlobalBizTypeEnum.REVIEW.getCode());
            mqMessageSendUtils.sendMqMessage(
                    SearchMqConstants.MILVUS_SYNC_EXCHANGE,
                    SearchMqConstants.MILVUS_SYNC_DELETE_ROUTING_KEY,
                    contentSyncMessage);

            if (Objects.equals(dbReview.getStatus(), ContentStatusEnum.PUBLISHED.getCode())) {
                evictUserReviewSourceIfNeeded(reviewType, dbReview);
            }
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
            // 2. 计数器不存在，从 review 表 COUNT 查询并回写 Redis
            count = query()
                    .eq("source_type", review.getSourceType())
                    .eq("source_id", review.getSourceId())
                    .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .count().intValue();
            redisService.setCacheObject(reviewCountKey, count);
            return count;
        }
        // 通用条件查询（后台管理等场景），直接走数据库
        return query()
                .eq(review.getSourceType() != null, "source_type", review.getSourceType())
                .eq(review.getSourceId() != null, "source_id", review.getSourceId())
                .eq(review.getUserId() != null, "user_id", review.getUserId())
                .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
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
                RedisConstants.LOCK_REVIEW_KEY,
                sourceIdList,
                Review.class,
                missingIds -> query()
                        .in("id", missingIds)
                        .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                        .eq("audit_status", AuditStatusEnum.PASS.getCode())
                        .list(),
                Review::getId,
                RedisConstants.CACHE_REVIEW_TTL,
                TimeUnit.MINUTES
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
            if (review != null
                    && Objects.equals(review.getStatus(), ContentStatusEnum.PUBLISHED.getCode())
                    && Objects.equals(review.getAuditStatus(), AuditStatusEnum.PASS.getCode())) {
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
        // 筛选出 sourceType 为商品类型的评价，收集其 sourceId 作为商品ID
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
                UserDTO::getId,
                Function.identity(),
                (v1, v2) -> v1
        ));
        reviewList.forEach(vo -> {
            UserDTO user = userMap.get(vo.getUserId());
            log.info("vo:{}",vo);
            if (user != null&&vo.getIsAnonymous()==false) {
                vo.setNickName(user.getNickName());
                vo.setUserIcon(user.getIcon());
            }else{
                vo.setUserId(null);
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
     * 批量同步落库方法（被 XXL-JOB 调度任务回调使用）：
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
     * 批量同步落库方法（被 XXL-JOB 调度任务回调使用）：
     * 同步数据库中评价的关注/收藏数量。
     *
     * @param updateMap K:璇勪环ID, V:鏈€鏂版敹钘忔暟
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
     * 聚合查询获取单条评价的完整详情。
     * 包含对防缓存击穿（逻辑过期/互斥锁）的底层封装调用，并聚合远端的关联业务数据。
     *
     * @param id 评价主键
     * @return 完整数据封装实体
     */
    @Override
    public ReviewVO getReviewById(Long id) {
        Review review = cacheClient.queryWithLogicalExpireAndPassThrough(
                RedisConstants.CACHE_REVIEW_KEY,
                RedisConstants.LOCK_REVIEW_KEY,
                id,
                Review.class,
                reviewId -> query()
                        .eq("id", reviewId)
                        .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                        .eq("audit_status", AuditStatusEnum.PASS.getCode())
                        .one(),
                RedisConstants.CACHE_REVIEW_TTL,
                java.util.concurrent.TimeUnit.MINUTES
        );
        if (review == null) {
            return null;
        }
        if (!Objects.equals(review.getStatus(), ContentStatusEnum.PUBLISHED.getCode())
                || !Objects.equals(review.getAuditStatus(), AuditStatusEnum.PASS.getCode())) {
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
        if (userDTO != null&&vo.getIsAnonymous()==false) {
            vo.setNickName(userDTO.getNickName());
            vo.setUserIcon(userDTO.getIcon());
        }else{
            vo.setUserId(null);
        }
        return vo;
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
     * 清理单条评价详情缓存
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
     * 涓昏鐢ㄤ簬鏁版嵁鎵归噺鍚屾钀藉簱鍚庣殑绾ц仈娓呯悊鍔ㄤ綔銆?
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
     * 保存评价 ID 到 Redis 排行榜中 (发布时的极简占位)
     * 涓氬姟瑙ｈ€︼細涓轰簡淇濊瘉鎺ュ彛鍝嶅簲閫熷害锛屽彧缁欐柊璇勪环璧嬩簣鏃堕棿鎴充綔涓哄垵濮嬪垎銆?
     * 精确的基于互动量与时间衰减的热度分数重算，完全移交至 SyncDataServiceImpl 异步处理。
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

        // 1. 【最新榜】：绝对准确。直接存入发布时间戳。
        redisService.setCacheZSet(newRankKey, review.getId().toString(), scoreTime);

        // 2. 【热门榜】：赋予一个极高的初始分（当前时间戳），保证用户刚发完评价能瞬间排在榜首。
        // 真实的“权重衰减”精细化算分，交由 XXL-JOB 定时任务稍后来洗牌。
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
                    .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .page(new Page<>(page, pageSize))
                    .getRecords();
            if (CollUtil.isEmpty(reviews)) {
                break;
            }
            int finalPage = page;
            executorService.submit(() -> {
                log.info("async sync review batch thread={}, page={}", Thread.currentThread().getName(), finalPage);
                sendReviewMilvusBatchMessage(reviews);
                log.info("缂傚倷鐒﹀畷妯衡枖閺囥垹鐓濈紒鍗炴櫒闂備焦瀵х粙鎴︽嚐椤栨縿浜归柛灞剧矋閺嗘粓鏌涢幇鍏告喚闁稿孩鍟坿濠碉紕鍋戦崐婵嗩焽瑜旈幃楣冾敆閸曨偆顓洪梺瑙勬緲婢у海绮堟径鎰厸濞达絽鍢插畵鍡涙煕?{}", Thread.currentThread().getName(), finalPage, reviews.size());
            });
            page++;
        }
        return "review sync task submitted";
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
            return "ids are required";
        }
        List<Long> idList = Arrays.stream(ids)
                .map(Long::valueOf)
                .collect(Collectors.toList());

        executorService.submit(() -> {
            List<Review> reviews = query()
                    .in("id", idList)
                    .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                    .eq("audit_status", AuditStatusEnum.PASS.getCode())
                    .list();
            if (CollUtil.isNotEmpty(reviews)) {
                sendReviewMilvusBatchMessage(reviews);
            }
        });
        return "publish task submitted";
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
        Integer businessStatus = null;
        if (Objects.equals(status, AuditStatusEnum.PASS.getCode())) {
            businessStatus = ContentStatusEnum.PUBLISHED.getCode();
        } else if (AuditStatusEnum.isRejected(status)) {
            businessStatus = ContentStatusEnum.OFF.getCode();
        }
        UpdateWrapper<Review> uw = new UpdateWrapper<Review>()
                .set("audit_status", status)
                .set("reject_reason", rejectReason)
                .eq("id", id);
        if (businessStatus != null) {
            uw.set("status", businessStatus);
        }
        boolean update = update(uw);
        if (update) {
            clearReviewCache(id);
            if (Objects.equals(status, AuditStatusEnum.PASS.getCode())) {
                publish(new String[]{id.toString()});
            } else {
                ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
                contentSyncMessage.setId(id);
                contentSyncMessage.setIndexName("review_index");
                contentSyncMessage.setType(GlobalBizTypeEnum.REVIEW.getCode());
                mqMessageSendUtils.sendMqMessage(
                        SearchMqConstants.MILVUS_SYNC_EXCHANGE,
                        SearchMqConstants.MILVUS_SYNC_DELETE_ROUTING_KEY,
                        contentSyncMessage);
            }
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

            // 将违规评价从排行榜中剔除
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
        if (redisService.getCacheZSetScore(userReviewKey, review.getSourceId().toString()) != null) {
            return true;
        }

        long count = query()
                .eq("user_id", userId)
                .eq("source_type", review.getSourceType())
                .eq("source_id", review.getSourceId())
                .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .count();
        if (count > 0) {
            redisService.setCacheZSet(userReviewKey, review.getSourceId().toString(), System.currentTimeMillis());
            return true;
        }
        return false;
    }

    @Override
    public ShopReviewAnalysisVO getShopReviewAnalysis(Long shopId, String startTime, String endTime) {
        if (shopId == null) {
            return buildEmptyShopReviewAnalysis();
        }
        java.time.LocalDateTime[] timeRange = parseReviewAnalysisTimeRange(startTime, endTime);
        ShopReviewAnalysisVO analysis = reviewMapper.selectShopReviewAnalysis(shopId, AuditStatusEnum.PASS.getCode(), timeRange[0], timeRange[1]);
        if (analysis == null) {
            analysis = buildEmptyShopReviewAnalysis();
        }
        normalizeShopReviewAnalysis(analysis);
        return analysis;
    }

    @Override
    public ShopReviewSuggestVO getShopReviewSuggest(Long shopId, String timeRange) {
        if (shopId == null) {
            return buildEmptyShopReviewSuggest();
        }
        java.time.LocalDateTime[] suggestRange = buildReviewSuggestRange(timeRange);
        ShopReviewAnalysisVO analysis = reviewMapper.selectShopReviewAnalysis(shopId, AuditStatusEnum.PASS.getCode(), suggestRange[0], suggestRange[1]);
        long pendingReviewCount = query()
                .eq("shop_id", shopId)
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
                .eq("reply_count", 0)
                .ge("create_time", java.sql.Timestamp.valueOf(suggestRange[0]))
                .le("create_time", java.sql.Timestamp.valueOf(suggestRange[1]))
                .count();
        ShopReviewSuggestVO suggest = buildEmptyShopReviewSuggest();
        suggest.setAvgScore(analysis == null || analysis.getAvgScore() == null ? java.math.BigDecimal.ZERO : analysis.getAvgScore().setScale(1, java.math.RoundingMode.HALF_UP));
        suggest.setBadReviewCount(analysis == null || analysis.getBadReviewCount() == null ? 0 : analysis.getBadReviewCount());
        suggest.setPendingReviewCount(Long.valueOf(pendingReviewCount).intValue());
        suggest.setBadReviewList(normalizeBadReviewList(reviewMapper.selectBadReviewList(shopId, AuditStatusEnum.PASS.getCode(), suggestRange[0], suggestRange[1], 1, 3, 10)));
        normalizeShopReviewSuggest(suggest);
        return suggest;
    }

    @Override
    public ShopReviewSuggestVO getShopReviewSuggestRealtime(Long shopId) {
        return getShopReviewSuggest(shopId, "month");
    }

    private java.time.LocalDateTime[] parseReviewAnalysisTimeRange(String startTime, String endTime) {
        if (startTime == null || endTime == null || startTime.isBlank() || endTime.isBlank()) {
            throw new com.smartLive.common.core.exception.BusinessException("startTime and endTime are required");
        }
        try {
            java.time.LocalDateTime start = java.time.LocalDateTime.parse(startTime, REVIEW_ANALYSIS_TIME_FORMATTER);
            java.time.LocalDateTime end = java.time.LocalDateTime.parse(endTime, REVIEW_ANALYSIS_TIME_FORMATTER);
            if (end.isBefore(start)) {
                throw new com.smartLive.common.core.exception.BusinessException("endTime must be greater than or equal to startTime");
            }
            return new java.time.LocalDateTime[]{start, end};
        } catch (java.time.format.DateTimeParseException ex) {
            throw new com.smartLive.common.core.exception.BusinessException("invalid time range format");
        }
    }

    private java.time.LocalDateTime[] buildReviewSuggestRange(String timeRange) {
        String normalized = timeRange == null || timeRange.isBlank() ? "week" : timeRange.trim().toLowerCase(java.util.Locale.ROOT);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return switch (normalized) {
            case "month" -> new java.time.LocalDateTime[]{now.withDayOfMonth(1).toLocalDate().atStartOfDay(), now};
            case "quarter" -> new java.time.LocalDateTime[]{now.minusDays(90), now};
            case "week" -> new java.time.LocalDateTime[]{now.toLocalDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay(), now};
            default -> throw new com.smartLive.common.core.exception.BusinessException("unsupported timeRange");
        };
    }

    private ShopReviewAnalysisVO buildEmptyShopReviewAnalysis() {
        return new ShopReviewAnalysisVO(java.math.BigDecimal.ZERO, 0);
    }

    private ShopReviewSuggestVO buildEmptyShopReviewSuggest() {
        return new ShopReviewSuggestVO(java.math.BigDecimal.ZERO, 0, 0, new ArrayList<>());
    }

    private void normalizeShopReviewAnalysis(ShopReviewAnalysisVO analysis) {
        if (analysis.getAvgScore() == null) {
            analysis.setAvgScore(java.math.BigDecimal.ZERO);
        }
        if (analysis.getBadReviewCount() == null) {
            analysis.setBadReviewCount(0);
        }
    }

    private ShopReviewSuggestVO normalizeShopReviewSuggest(ShopReviewSuggestVO suggest) {
        if (suggest.getAvgScore() == null) {
            suggest.setAvgScore(java.math.BigDecimal.ZERO);
        }
        if (suggest.getBadReviewCount() == null) {
            suggest.setBadReviewCount(0);
        }
        if (suggest.getPendingReviewCount() == null) {
            suggest.setPendingReviewCount(0);
        }
        if (suggest.getBadReviewList() == null) {
            suggest.setBadReviewList(new ArrayList<>());
        }
        return suggest;
    }

    private List<BadReviewVO> normalizeBadReviewList(List<BadReviewVO> badReviewList) {
        if (badReviewList == null) {
            return new ArrayList<>();
        }
        badReviewList.forEach(item -> {
            if (item.getContent() == null) {
                item.setContent("");
            }
            if (item.getScore() == null) {
                item.setScore(0);
            }
        });
        return badReviewList;
    }

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
                .eq("status", ContentStatusEnum.PUBLISHED.getCode())
                .eq("audit_status", AuditStatusEnum.PASS.getCode())
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
            throw new RuntimeException("reviews must not be empty");
        }
        redisService.deleteObject(redisService.keys(RedisConstants.CACHE_AI_REVIEW_KEY + "*"));
        reviews.forEach(reviewDTO -> {
            String key = RedisConstants.CACHE_AI_REVIEW_KEY + reviewDTO.getSourceType() + ":" + reviewDTO.getSourceId();
            redisService.setCacheObject(key, com.alibaba.fastjson.JSON.toJSONString(reviewDTO));
        });
        return true;
    }
}
