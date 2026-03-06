package com.smartLive.interaction.service.impl;
import com.smartLive.common.core.constant.mq.AiAuditMqConstants;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.blog.api.DTO.BlogDTO;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.AppLoginUser;
import com.smartLive.common.core.enums.AuditStatusEnum;
import com.smartLive.common.core.enums.CommentTypeEnum;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.RankRedisEnum;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.rabbitmq.domain.AuditMessage;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.common.redis.util.RedisMultiCacheManager;
import com.smartLive.common.redis.util.ZSetIdManager;
import com.smartLive.interaction.api.DTO.LikeDTO;
import com.smartLive.interaction.domain.AIGenerateRequest;
import com.smartLive.interaction.domain.BO.AuditCommentBO;
import com.smartLive.interaction.domain.Comment;
import com.smartLive.interaction.domain.VO.CommentVO;
import com.smartLive.interaction.mapper.CommentMapper;
import com.smartLive.interaction.service.ICommentService;
import com.smartLive.interaction.service.ILikeService;
import com.smartLive.interaction.strategy.factory.ResourceStrategyFactory;
import com.smartLive.interaction.strategy.resource.ResourceStrategy;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.DTO.ShopDTO;
import com.smartLive.user.api.RemoteAppUserService;
import org.springframework.beans.BeanUtils;
import com.smartLive.user.api.domain.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 评论服务核心实现类
 *
 * 架构说明：
 * 1. 坚持“单一职责原则”：本类专注于评论数据的 CRUD、组装外部依赖（RPC）、以及发送数据到 Redis/MQ。
 * 2. 动静分离设计：所有评论详细内容大一统存储在 CACHE_COMMENT_KEY 中，排行榜 ID 列表按业务隔离开。
 * 3. 级联异步计算解耦：去除了所有手动的父级分数计算流转，统交由 SyncDataServiceImpl 后台通过 Sync 队列级联到 Calc 队列进行全自动处理。
 *
 * @author mumulin
 * @date 2025-09-21
 */
@Service
@Slf4j
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements ICommentService {
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private RemoteAppUserService remoteAppUserService;

    @Autowired
    private MqMessageSendUtils mqMessageSendUtils;
    @Autowired
    private RedisService redisService;
    @Autowired
    private ZSetIdManager zSetIdManager;
    @Autowired
    private RedisMultiCacheManager redisMultiCacheManager;
    //引入线程池
    @Autowired
    private ExecutorService executorService;

    private ResourceStrategyFactory resourceStrategyFactory;
    private ILikeService likeService;

    @Autowired
    public CommentServiceImpl(@Lazy ILikeService iLikeService, @Lazy ResourceStrategyFactory resourceStrategyFactory) {
        this.likeService = iLikeService;
        this.resourceStrategyFactory = resourceStrategyFactory;
    }

    /**
     * 将Comment实体转换为CommentVO
     */
    private CommentVO convertToCommentVO(Comment comment) {
        if (comment == null) {
            return null;
        }
        CommentVO commentVO = new CommentVO();
        BeanUtils.copyProperties(comment, commentVO);
        return commentVO;
    }

    /**
     * 将Comment列表转换为CommentVO列表
     */
    private List<CommentVO> convertToCommentVOList(List<Comment> commentList) {
        if (CollUtil.isEmpty(commentList)) {
            return new ArrayList<>();
        }
        return commentList.stream()
                .map(this::convertToCommentVO)
                .collect(Collectors.toList());
    }

    /**
     * 根据 ID 查询单条评论（直接查库）
     *
     * @param id 评论主键
     * @return 评论实体
     */
    @Override
    public Comment selectCommentById(Long id) {
        return commentMapper.selectCommentById(id);
    }

    /**
     * 根据条件查询评论列表（常用于后台管理）
     *
     * @param comment 查询条件
     * @return 评论集合
     */
    @Override
    public List<Comment> selectCommentList(Comment comment) {
        return commentMapper.selectCommentList(comment);
    }

    /**
     * 底层新增评论数据
     *
     * @param comment 评论实体
     * @return 影响行数
     */
    @Override
    public int insertComment(Comment comment) {
        comment.setCreateTime(DateUtils.getNowDate());
        return commentMapper.insertComment(comment);
    }

    /**
     * 更新评论数据
     *
     * @param comment 评论实体
     * @return 影响行数
     */
    @Override
    public int updateComment(Comment comment) {
        comment.setUpdateTime(DateUtils.getNowDate());
        return commentMapper.updateComment(comment);
    }

    /**
     * 批量物理删除评论
     *
     * @param ids 评论主键数组
     * @return 影响行数
     */
    @Override
    public int deleteCommentByIds(Long[] ids) {
        return commentMapper.deleteCommentByIds(ids);
    }

    /**
     * 物理删除单条评论
     *
     * @param id 评论主键
     * @return 影响行数
     */
    @Override
    public int deleteCommentById(Long id) {
        return commentMapper.deleteCommentById(id);
    }

    /**
     * 【核心读链路】分页获取前台展示的评论列表
     * 采用 "ID List (ZSet) + 详情缓存 (Cache)" 的大厂标准架构，极致抗压。
     *
     * @param comment 包含目标源信息
     * @param current 当前页码
     * @return 评论视图列表
     */
    @Override
    public List<CommentVO> listComment(Comment comment, Integer current, String sort) {
        CommentTypeEnum commentType = CommentTypeEnum.getByCode(comment.getSourceType());
        if (commentType == null) {
            log.error("参数错误：未知的评论类型");
            return Collections.emptyList();
        }

        // 1. 尝试从 Redis 排行榜拉取排好序的 ID
        RankRedisEnum hotRankRedisEnum = RankRedisEnum.getByCategoryAndCode("COMMENT", commentType.getCode());
        String commentKeyPrefix = "";
        if (hotRankRedisEnum != null) {
             if ("latest".equals(sort)) {
                 commentKeyPrefix = hotRankRedisEnum.getNewRankKeyPrefix();
             } else {
                 commentKeyPrefix = hotRankRedisEnum.getHotRankKeyPrefix();
             }
        }
        Page<Long> longPage = zSetIdManager.pageIds(commentKeyPrefix, comment.getSourceId(), current, SystemConstants.MAX_PAGE_SIZE);
        List<Long> commentIdList = longPage.getRecords();
        List<CommentVO> voList = new ArrayList<>();

        if (commentIdList != null && !commentIdList.isEmpty()) {
            // 去统一详情池捞取具体数据
            voList = getCommentListByIds(commentIdList);
        }

        // 兜底逻辑：缓存击穿时查库并重建 ZSet 榜单
        if (voList == null || voList.isEmpty()) {
            log.info("从数据库中获取评论数据");
            var q = query()
                    .eq("source_id", comment.getSourceId())
                    .ne("status", 2)
                    .ne("status",3)
                    .eq("source_type", comment.getSourceType());
            if ("latest".equals(sort)) {
                 q.orderByDesc("create_time");
            } else {
                 q.orderByDesc("liked").orderByDesc("create_time");
            }
            List<Comment> list = q.list();
            if (!list.isEmpty()) {
                final List<Comment> finalList = list;
                executorService.execute(() -> {
                    log.info("重建 ZSet 榜单");
                    RankRedisEnum redisEnum = RankRedisEnum.getByCategoryAndCode("COMMENT", commentType.getCode());
                    String hotRankKey = redisEnum.getHotRankKeyPrefix() + comment.getSourceId();
                    String newRankKey = redisEnum.getNewRankKeyPrefix() + comment.getSourceId();
                    zSetIdManager.saveToZSet(hotRankKey, finalList, Comment::getId, Comment::getCreateTime);
                    zSetIdManager.saveToZSet(newRankKey, finalList, Comment::getId, Comment::getCreateTime);

                    // 【核心补充】将这批临时用 createTime 重建排行榜的评论推入排队，等待异步精细算分矫正
                    if (redisEnum.getCalcQueueKey() != null) {
                        redisService.setCacheSet(redisEnum.getCalcQueueKey(), finalList.stream().map(c -> String.valueOf(c.getId())).collect(Collectors.toSet()));
                    }
                });

                List<Comment> pageList = list.size() > SystemConstants.MAX_PAGE_SIZE ? list.subList((current - 1) * SystemConstants.MAX_PAGE_SIZE, (current - 1) * SystemConstants.MAX_PAGE_SIZE + SystemConstants.MAX_PAGE_SIZE) : list;
                voList = convertToCommentVOList(pageList);
                queryCommentListIsLike(voList);
                queryCommentListUserMessage(voList);
            }
        }

        if (voList == null) {
            return Collections.emptyList();
        }

        return voList;
    }

    /**
     * 获取楼中楼（子评论）列表
     *
     * @param comment 查询条件（主要包含 answer_id 父评论ID）
     * @param current 当前页码
     * @return 子评论列表
     */
    @Override
    public List<CommentVO> listChildComment(Comment comment, Integer current) {
        List<Comment> commentList = query()
                .eq("answer_id", comment.getId())
                .ne("status", "2")
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE))
                .getRecords();
        if (commentList != null && !commentList.isEmpty()) {
            List<CommentVO> voList = convertToCommentVOList(commentList);
            queryCommentListIsLike(voList);
            queryCommentListUserMessage(voList);
            return voList;
        }
        return Collections.emptyList();
    }

    /**
     * 【核心写链路】前端发布新增评论
     * 将繁重计算剥离，利用双轨制异步队列保证 C 端响应的极速体验。
     * 父级分数变化的触发全部交由 SyncDataServiceImpl 的回调处理完成闭环。
     *
     * @param comment 评论实体
     * @return 影响行数
     */
    @Override
    @Transactional
    public Integer addComment(Comment comment) {
        comment.setCreateTime(DateUtils.getNowDate());
        int i = commentMapper.insertComment(comment);
        if (i > 0) {
            CommentTypeEnum commentType = CommentTypeEnum.getByCode(comment.getSourceType());
            if (commentType == null) {
                log.error("commentType is null, sourceType={}", comment.getSourceType());
                return i;
            }
            String commentCountKeyPrefix = commentType.getCommentCountKeyPrefix() + comment.getSourceId();
            String commentSyncKey = commentType.getCommentSyncKey();
            String userCommentKey = userCommentKey(commentType, comment.getUserId());

            // 1. 将新评论存入排行榜（时间戳占位，重排序交由 XXL-JOB）
            saveCommentRankToRedis(commentType, comment);

            // 2. 记录用户的足迹
            if (userCommentKey != null) {
                redisService.setCacheZSet(userCommentKey, comment.getSourceId().toString(), System.currentTimeMillis());
            }

            // 3. 确保 Redis 计数器已初始化，再自增该目标主体的评论总数
            getCommentCount(comment);
            redisService.incrementCacheValue(commentCountKeyPrefix);

            // 4. 【数据落库轨】：记录有变化的目标源，交由定时任务批量更新 MySQL。
            // 注意：这里将目标源的 ID 放入了 Sync 队列，定时任务同步完数据后，会自动级联触发该目标源的算分逻辑。
            redisService.setCacheSet(commentSyncKey, Collections.singleton(comment.getSourceId().toString()));

            // 5. 【热度计算轨】：新产生的评论自己也需要进行一次初始热度算分。
            RankRedisEnum hotRankRedisEnum = RankRedisEnum.getByCategoryAndCode("COMMENT", commentType.getCode());
            if (hotRankRedisEnum != null) {
                redisService.setCacheSet(hotRankRedisEnum.getCalcQueueKey(), comment.getId().toString());
            }

            // 6. 发送审核消息队列
            sendAuditMessage(comment);
        }
        return i;
    }

    /**
     * 封装发送审核 MQ 消息
     *
     * @param comment 待审核的评论
     */
    private void sendAuditMessage(Comment comment) {
        ResourceStrategy resourceType = resourceStrategyFactory.getStrategy(comment.getSourceType());
        HashMap<String, String> content = resourceType.getResourceContentById(comment.getSourceId());
        AuditCommentBO auditCommentBO = new AuditCommentBO();
        BeanUtil.copyProperties(comment, auditCommentBO);
        auditCommentBO.setTargetTitle(content.get("title"));
        auditCommentBO.setTargetImages(content.get("images"));
        AuditMessage auditMessage = AuditMessage.builder()
                .bizId(comment.getId())
                .bizType(GlobalBizTypeEnum.COMMENT.getCode())
                .submitterId(comment.getUserId())
                .auditContent(BeanUtil.beanToMap(auditCommentBO))
                .createTime(comment.getCreateTime())
                .build();
        mqMessageSendUtils.sendMqMessage( AiAuditMqConstants.AUDIT_DIRECT_EXCHANGE, AiAuditMqConstants.AUDIT_ROUTING_KEY, auditMessage);
    }

    /**
     * 用户前端逻辑删除自己的评论
     *
     * @param comment 包含要删除 ID 的评论实体
     * @return 是否成功
     */
    @Override
    @Transactional
    public Boolean deleteComment(Comment comment) {
        Comment dbComment = getById(comment.getId());
        if (dbComment == null) {
            return false;
        }
        boolean i = removeById(dbComment.getId());
        if (i) {
            CommentTypeEnum commentType = CommentTypeEnum.getByCode(dbComment.getSourceType());
            if (commentType == null) {
                return true;
            }
            RankRedisEnum hotRankRedisEnum = RankRedisEnum.getByCategoryAndCode("COMMENT", commentType.getCode());
            String commentKeyPrefix = hotRankRedisEnum.getHotRankKeyPrefix() + dbComment.getSourceId();
            String commentNewRankKeyPrefix = hotRankRedisEnum.getNewRankKeyPrefix() + dbComment.getSourceId();
            String commentCountKeyPrefix = commentType.getCommentCountKeyPrefix() + dbComment.getSourceId();
            String commentSyncKey = commentType.getCommentSyncKey();

            // 清理缓存排行榜与计数器
            redisService.removeCacheZSetObject(commentKeyPrefix, dbComment.getId().toString());
            redisService.removeCacheZSetObject(commentNewRankKeyPrefix, dbComment.getId().toString());
            // 确保 Redis 计数器已初始化，再递减
            getCommentCount(dbComment);
            redisService.decrementCacheValue(commentCountKeyPrefix);

            // 双轨制触发：同步扣减 MySQL 计数。
            // 同样，定时任务在同步完成后会自动级联触发父级主体的降权算分，无需在此处手动操作。
            redisService.setCacheSet(commentSyncKey, Collections.singleton(dbComment.getSourceId().toString()));

            evictUserCommentSourceIfNeeded(commentType, dbComment);
        }
        return i;
    }

    /**
     * 获取指定用户的评论历史足迹
     *
     * @param comment 查询条件封装
     * @param current 当前页码
     * @return 评论历史列表
     */
    @Override
    public List<CommentVO> getCommentOfUser(Comment comment, Integer current) {
        if (comment == null) {
            return Collections.emptyList();
        }
        Long userId = comment.getUserId();
        if (userId == null) {
            AppLoginUser user = UserContextHolder.getUser();
            if (user != null) {
                userId = user.getId();
                comment.setUserId(userId);
            }
        }
        if (userId == null) {
            return Collections.emptyList();
        }

        int pageNo = current == null || current < 1 ? 1 : current;
        int pageSize = SystemConstants.MAX_PAGE_SIZE;
        CommentTypeEnum commentType = CommentTypeEnum.getByCode(comment.getSourceType());
        List<CommentVO> voList = Collections.emptyList();
        int redisSourceCount = 0;

        if (commentType != null) {
            Page<Long> idPage = zSetIdManager.pageIds(commentType.getUserCommentKeyPrefix() + commentType.getCode() + ":", userId, pageNo, pageSize);
            List<Long> sourceIdList = idPage.getRecords();
            redisSourceCount = CollUtil.isEmpty(sourceIdList) ? 0 : sourceIdList.size();
            if (CollUtil.isNotEmpty(sourceIdList)) {
                voList = getCommentListByIds(sourceIdList);
            }
        }
        if (redisSourceCount > 0 && voList.size() < redisSourceCount) {
            voList = Collections.emptyList();
        }
        if (CollUtil.isEmpty(voList)) {
            List<Comment> dbList = query()
                    .eq(comment.getSourceType() != null, "source_type", comment.getSourceType())
                    .eq("parent_id", 0)
                    .eq("user_id", userId)
                    .orderByDesc("liked")
                    .orderByDesc("create_time")
                    .list();

            if (CollUtil.isEmpty(dbList)) {
                return Collections.emptyList();
            }

            if (commentType != null) {
                zSetIdManager.saveToZSet(userCommentKey(commentType, userId), dbList, Comment::getId, Comment::getCreateTime);
            }
            int start = (pageNo - 1) * pageSize;
            if (start >= dbList.size()) {
                return Collections.emptyList();
            }
            int end = Math.min(start + pageSize, dbList.size());
            List<Comment> pageList = dbList.subList(start, end);
            voList = convertToCommentVOList(pageList);
        }
        enrichUserCommentList(voList, userId);
        return voList;
    }

    /**
     * 为用户视角的评论列表组装冗余的业务显示数据（如被评论的文章标题）
     *
     * @param list   基础评论列表
     * @param userId 目标用户ID
     */
    private void enrichUserCommentList(List<CommentVO> list, Long userId) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        UserDTO owner = remoteAppUserService.queryUserById(userId);
        for (CommentVO c : list) {
            if (owner != null) {
                c.setNickName(owner.getNickName());
                c.setUserIcon(owner.getIcon());
            }

            ResourceStrategy strategy = resourceStrategyFactory.getStrategy(c.getSourceType());
            if (strategy != null) {
                HashMap<String, String> content = strategy.getResourceContentById(c.getSourceId());
                if (content != null) {
                    c.setSourceName(content.get("title"));
                    c.setShopImages(content.get("images"));
                }
            }
        }
    }

    /**
     * 获取全量系统评论集合
     */
    @Override
    public List<CommentVO> getCommentList() {
        List<Comment> list = query().list();
        List<CommentVO> voList = convertToCommentVOList(list);
        voList.forEach(c -> {
            ResourceStrategy strategy = resourceStrategyFactory.getStrategy(c.getSourceType());
            if (strategy != null) {
                HashMap<String, String> content = strategy.getResourceContentById(c.getSourceId());
                if (content != null) {
                    c.setSourceName(content.get("title"));
                    c.setShopImages(content.get("images"));
                }
            }
        });
        return voList;
    }



    /**
     * 获取指定条件下的评论总数
     * 优先级: Redis 独立计数器 → comment 表 COUNT
     */
    @Override
    public Integer getCommentCount(Comment comment) {
        CommentTypeEnum commentType = CommentTypeEnum.getByCode(comment.getSourceType());
        // 只有同时指定了 sourceType 和 sourceId 时才走 Redis 计数器
        if (commentType != null && comment.getSourceId() != null) {
            String commentCountKey = commentType.getCommentCountKeyPrefix() + comment.getSourceId();
            // 1. 从 Redis 独立计数器读取
            Integer count = redisService.getCacheObject(commentCountKey);
            if (count != null) {
                return count;
            }
            // 2. 计数器不存在，从 comment 表 COUNT 查询并回写 Redis
            count = query()
                    .eq("source_type", comment.getSourceType())
                    .eq("source_id", comment.getSourceId())
                    .count().intValue();
            redisService.setCacheObject(commentCountKey, count);
            return count;
        }
        // 通用条件查询（后台管理等场景），直接走数据库
        return query()
                .eq(comment.getSourceType() != null, "source_type", comment.getSourceType())
                .eq(comment.getSourceId() != null, "source_id", comment.getSourceId())
                .eq(comment.getUserId() != null, "user_id", comment.getUserId())
                .count().intValue();
    }

    /**
     * 获取系统全量评论数
     */
    @Override
    public Integer getCommentTotal() {
        return query().count().intValue();
    }

    /**
     * 触发异步收集聚合 AI 需要生成的任务列表
     */
    @Override
    public void aiCreateComment() {
        List<AIGenerateRequest> list = query().list().stream()
                .collect(Collectors.groupingBy(
                        Comment::getSourceType,
                        Collectors.mapping(
                                Comment::getSourceId,
                                Collectors.collectingAndThen(
                                        Collectors.toCollection(LinkedHashSet::new),
                                        ArrayList::new
                                )
                        )
                ))
                .entrySet().stream()
                .map(entry -> new AIGenerateRequest(
                        entry.getKey(),
                        entry.getValue()
                ))
                .collect(Collectors.toList());
        mqMessageSendUtils.sendMqMessage(AiAuditMqConstants.AI_EXCHANGE_NAME, AiAuditMqConstants.AI_COMMENT_ROUTING, list);
    }

    /**
     * 批量从 Redis 详情池中根据 ID 获取评论详情
     *
     * @param sourceIdList 评论主键集合
     * @return 对应的评论实体
     */
    @Override
    public List<CommentVO> getCommentListByIds(List<Long> sourceIdList) {
        if (CollUtil.isEmpty(sourceIdList)) {
            return Collections.emptyList();
        }

        List<Comment> list = redisMultiCacheManager.queryBatchWithCache(
                RedisConstants.CACHE_COMMENT_KEY,
                sourceIdList,
                Comment.class,
                missingIds -> query().in("id", missingIds).list(),
                Comment::getId,
                RedisConstants.CACHE_COMMENT_TTL,
                TimeUnit.MINUTES
        );
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }

        Map<Long, Comment> commentMap = list.stream()
                .filter(comment -> comment != null && comment.getId() != null)
                .collect(Collectors.toMap(Comment::getId, comment -> comment, (v1, v2) -> v1));
        List<CommentVO> orderedList = new ArrayList<>(sourceIdList.size());
        for (Long id : sourceIdList) {
            Comment comment = commentMap.get(id);
            if (comment != null) {
                CommentVO vo = convertToCommentVO(comment);
                ResourceStrategy strategy = resourceStrategyFactory.getStrategy(comment.getSourceType());
                if (strategy != null) {
                    HashMap<String, String> content = strategy.getResourceContentById(comment.getSourceId());
                    if (content != null) {
                        vo.setSourceName(content.get("title"));
                    }
                }
                orderedList.add(vo);
            }
        }
        queryCommentListIsLike(orderedList);
        queryCommentListUserMessage(orderedList);
        return orderedList;
    }

    /**
     * RPC 调用装配评论对应的用户昵称与头像
     *
     * @param commentList 原始评论集合
     */
    private void queryCommentListUserMessage(List<CommentVO> commentList) {
        if (CollUtil.isEmpty(commentList)) {
            return;
        }
        List<Long> userIds = commentList.stream()
                .map(CommentVO::getUserId)
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
        commentList.forEach(vo -> {
            UserDTO user = userMap.get(vo.getUserId());
            if (user != null) {
                vo.setNickName(user.getNickName());
                vo.setUserIcon(user.getIcon());
            }
        });
    }

    /**
     * 批量校验并装载当前登录用户对列表中评论的点赞状态
     *
     * @param commentList 原始评论集合
     */
    private void queryCommentListIsLike(List<CommentVO> commentList) {
        if (CollUtil.isEmpty(commentList)) {
            return;
        }
        AppLoginUser user = UserContextHolder.getUser();
        if (user == null) {
            commentList.forEach(vo -> {
                if (vo != null) {
                    vo.setIsLike(false);
                }
            });
            return;
        }

        List<Long> commentIds = commentList.stream()
                .map(CommentVO::getId)
                .collect(Collectors.toList());
        LikeDTO likeDTO = new LikeDTO();
        likeDTO.setUserId(user.getId());
        likeDTO.setSourceType(GlobalBizTypeEnum.COMMENT.getCode());
        Map<Long, Boolean> likeMap = likeService.isLikeBatch(likeDTO, commentIds);

        commentList.forEach(vo -> {
            if (vo != null) {
                vo.setIsLike(likeMap.getOrDefault(vo.getId(), false));
            }
        });
    }

    /**
     * 批量同步落库点赞数（被 XXL-JOB 调用）
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
        return true;
    }

    /**
     * 批量同步落库子评论回复数（被 XXL-JOB 调用）
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
        return true;
    }

    /**
     * 获取特定目标的总点赞数
     */
    @Override
    public Integer getCommentLikeCount(Long sourceId) {
        Comment comment = getById(sourceId);
        return comment.getLiked();
    }

    /**
     * 获取单条评论的详细内容并挂载关联的外部冗余信息
     */
    @Override
    public CommentVO getCommentById(Long id) {
        Comment comment = getById(id);
        if (comment == null) {
            return null;
        }
        CommentVO vo = convertToCommentVO(comment);
        ResourceStrategy strategy = resourceStrategyFactory.getStrategy(comment.getSourceType());
        if (strategy != null) {
            HashMap<String, String> content = strategy.getResourceContentById(comment.getSourceId());
            if (content != null) {
                vo.setSourceName(content.get("title"));
                vo.setShopImages(content.get("images"));
            }
        }

        UserDTO userDTO = remoteAppUserService.queryUserById(comment.getUserId());
        if (userDTO != null) {
            vo.setNickName(userDTO.getNickName());
            vo.setUserIcon(userDTO.getIcon());
        }
        return vo;
    }

    /**
     * 运营后台处理违规审核逻辑
     */
    @Override
    public Boolean updateCommentStatus(Long id, Integer status) {
        boolean update = update(new UpdateWrapper<Comment>()
                .set("status", status)
                .eq("id", id));
        // 如果审核不通过，直接将数据移出排行榜并触发其父级目标的重计算扣分
        if (update && AuditStatusEnum.isRejected(status)) {
            Comment comment = getById(id);
            CommentTypeEnum commentType = CommentTypeEnum.getByCode(comment.getSourceType());
            RankRedisEnum hotRankRedisEnum = RankRedisEnum.getByCategoryAndCode("COMMENT", commentType.getCode());
            String commentKeyPrefix = hotRankRedisEnum.getHotRankKeyPrefix() + comment.getSourceId();
            String commentNewRankKeyPrefix = hotRankRedisEnum.getNewRankKeyPrefix() + comment.getSourceId();
            String commentCountKeyPrefix = commentType.getCommentCountKeyPrefix() + comment.getSourceId();
            String commentSyncKey = commentType.getCommentSyncKey();

            redisService.removeCacheZSetObject(commentKeyPrefix, comment.getId().toString());
            redisService.removeCacheZSetObject(commentNewRankKeyPrefix, comment.getId().toString());
            // 确保 Redis 计数器已初始化，再递减
            getCommentCount(comment);
            redisService.decrementCacheValue(commentCountKeyPrefix);

            // 将所属父级目标扔入同步与级联队列
            redisService.setCacheSet(commentSyncKey, Collections.singleton(comment.getSourceId().toString()));

            evictUserCommentSourceIfNeeded(commentType, comment);
        }
        return update;
    }

    /**
     * 校验当前登录用户是否操作过当前目标业务源
     */
    @Override
    public Boolean isComment(Comment comment) {
        if (comment == null || comment.getSourceType() == null || comment.getSourceId() == null) {
            return false;
        }
        Long userId = comment.getUserId();
        if (userId == null) {
            AppLoginUser user = UserContextHolder.getUser();
            if (user != null) {
                userId = user.getId();
            }
        }
        if (userId == null) {
            return false;
        }
        CommentTypeEnum commentType = CommentTypeEnum.getByCode(comment.getSourceType());
        if (commentType == null) {
            return false;
        }
        String userCommentKey = userCommentKey(commentType, userId);
        if (userCommentKey == null) {
            return false;
        }
        if (redisService.getCacheZSetScore(userCommentKey, comment.getSourceId().toString()) != null) {
            return true;
        }

        long count = query()
                .eq("user_id", userId)
                .eq("source_type", comment.getSourceType())
                .eq("source_id", comment.getSourceId())
                .ne("status", "2")
                .count();
        if (count > 0) {
            redisService.setCacheZSet(userCommentKey, comment.getSourceId().toString(), System.currentTimeMillis());
            return true;
        }
        return false;
    }

    /**
     * 新增评论时将自身 ID 加入 Redis 排行榜（极简占位版）
     */
    private void saveCommentRankToRedis(CommentTypeEnum commentType, Comment comment) {
        if (commentType == null || comment == null || comment.getId() == null || comment.getSourceId() == null) {
            return;
        }
        long scoreTime = comment.getCreateTime() == null ? System.currentTimeMillis() : comment.getCreateTime().getTime();
        RankRedisEnum hotRankRedisEnum = RankRedisEnum.getByCategoryAndCode("COMMENT", commentType.getCode());
        String hotRankKey = hotRankRedisEnum.getHotRankKeyPrefix() + comment.getSourceId();
        String newRankKey = hotRankRedisEnum.getNewRankKeyPrefix() + comment.getSourceId();

        // 赋予最新榜绝对的时间戳，赋予热门榜极高的临时分保证优先曝光
        redisService.setCacheZSet(newRankKey, comment.getId().toString(), scoreTime);
        redisService.setCacheZSet(hotRankKey, comment.getId().toString(), (double) scoreTime);
    }

    /**
     * 抽取获取用户针对目标源的已评足迹缓存 Key
     */
    private String userCommentKey(CommentTypeEnum commentType, Long userId) {
        if (commentType == null || userId == null) {
            return null;
        }
        return commentType.getUserCommentKeyPrefix() + commentType.getCode() + ":" + userId;
    }

    /**
     * 若评论被管理员清空，连带清理其在 Redis 中的“已操作”记录
     */
    private void evictUserCommentSourceIfNeeded(CommentTypeEnum commentType, Comment comment) {
        if (commentType == null || comment == null || comment.getUserId() == null || comment.getSourceId() == null) {
            return;
        }
        long remains = query()
                .eq("user_id", comment.getUserId())
                .eq("source_type", comment.getSourceType())
                .eq("source_id", comment.getSourceId())
                .ne("status", "2")
                .count();
        if (remains <= 0) {
            redisService.removeCacheZSetObject(userCommentKey(commentType, comment.getUserId()), comment.getSourceId().toString());
        }
    }
}
