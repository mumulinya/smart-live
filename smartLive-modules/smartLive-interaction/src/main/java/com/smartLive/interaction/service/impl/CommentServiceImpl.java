package com.smartLive.interaction.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.blog.api.DTO.BlogDTO;
import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.enums.AuditStatusEnum;
import com.smartLive.common.core.enums.CommentTypeEnum;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.rabbitmq.domain.AuditMessage;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.interaction.domain.AIGenerateRequest;
import com.smartLive.interaction.domain.BO.AuditCommentBO;
import com.smartLive.interaction.domain.Comment;
import com.smartLive.interaction.domain.Like;
import com.smartLive.interaction.mapper.CommentMapper;
import com.smartLive.interaction.service.ICommentService;
import com.smartLive.interaction.service.ILikeService;
import com.smartLive.interaction.strategy.factory.ResourceStrategyFactory;
import com.smartLive.interaction.strategy.resource.ResourceStrategy;
import com.smartLive.interaction.tool.QueryRedisSourceIdsTool;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.DTO.ShopDTO;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartLive.user.api.domain.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论Service业务层处理
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
    private RemoteBlogService remoteBlogService;

    @Autowired
    private RemoteShopService remoteShopService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private QueryRedisSourceIdsTool queryRedisSourceIdsTool;
    @Autowired
    private RedisService redisService;
    private ResourceStrategyFactory resourceStrategyFactory;
    private ILikeService iLikeService;


    @Autowired
    public CommentServiceImpl(@Lazy ILikeService iLikeService, @Lazy ResourceStrategyFactory resourceStrategyFactory) {
        this.iLikeService = iLikeService;
        this.resourceStrategyFactory = resourceStrategyFactory;
    }
    /**
     * 查询评论
     *
     * @param id 评论主键
     * @return 评论
     */
    @Override
    public Comment selectCommentById(Long id) {
        return commentMapper.selectCommentById(id);
    }

    /**
     * 查询评论列表
     *
     * @param comment 评论
     * @return 评论
     */
    @Override
    public List<Comment> selectCommentList(Comment comment) {
        return commentMapper.selectCommentList(comment);
    }

    /**
     * 新增评论
     *
     * @param comment 评论
     * @return 结果
     */
    @Override
    public int insertComment(Comment comment) {
        comment.setCreateTime(DateUtils.getNowDate());
        return commentMapper.insertComment(comment);
    }

    /**
     * 修改评论
     *
     * @param comment 评论
     * @return 结果
     */
    @Override
    public int updateComment(Comment comment) {
        comment.setUpdateTime(DateUtils.getNowDate());
        return commentMapper.updateComment(comment);
    }

    /**
     * 批量删除评论
     *
     * @param ids 需要删除的评论主键
     * @return 结果
     */
    @Override
    public int deleteCommentByIds(Long[] ids) {
        return commentMapper.deleteCommentByIds(ids);
    }

    /**
     * 删除评论信息
     *
     * @param id 评论主键
     * @return 结果
     */
    @Override
    public int deleteCommentById(Long id) {
        return commentMapper.deleteCommentById(id);
    }

    /**
     * 获取评论列表
     *
     * @param comment
     * @return
     */
    @Override
    public List<Comment> listComment(Comment comment, Integer current) {
        //从redis里面获取
        CommentTypeEnum commentType = CommentTypeEnum.getByCode(comment.getSourceType());
        if (commentType == null) {
            log.error("参数错误");
            return Collections.emptyList();
        }
        String commentKeyPrefix = commentType.getCommentKeyPrefix();
        Page<Long> longPage = queryRedisSourceIdsTool.queryRedisIdPage(commentKeyPrefix, comment.getSourceId(), current, SystemConstants.MAX_PAGE_SIZE);
        List<Long> commentIdList = longPage.getRecords();
        List<Comment> list=new ArrayList<>();
        if(commentIdList != null && commentIdList.size() > 0){
             list = lambdaQuery()
                     .ne(Comment::getStatus, "2")
                     .in(Comment::getId, commentIdList)
                     .orderByDesc(Comment::getLiked).list();
        }
        //如果redis里面没有数据，则从数据库里面获取
        if(list == null || list.size() == 0){
            log.info("从数据库里面获取");
             list = query()
                    .eq("source_id", comment.getSourceId())
                     .ne("status", "2")
                     .eq("source_type", comment.getSourceType())
                    .orderByDesc("liked")
                    .list();
            //截取
            if(!list.isEmpty()){
                //保存到redis里面
                saveCommentListToRedis(commentKeyPrefix+comment.getSourceId(), list);
                //截取当前页
                list = list.size() > SystemConstants.DEFAULT_PAGE_SIZE ? list.subList((current-1)*SystemConstants.DEFAULT_PAGE_SIZE, (current-1)*SystemConstants.DEFAULT_PAGE_SIZE + SystemConstants.DEFAULT_PAGE_SIZE) : list;
            }
        }
        if(list == null){
            return Collections.emptyList();
        }
        list.stream().forEach(c -> {
            Long id = c.getUserId();
            //判断是否点赞
            Like like = new Like();
            like.setSourceType(GlobalBizTypeEnum.COMMENT.getCode());
            like.setSourceId(c.getId());
            c.setIsLike(iLikeService.isLike(like));
            UserDTO user = remoteAppUserService.queryUserById(id);
            if (user != null) {
                c.setNickName(user.getNickName());
                c.setUserIcon(user.getIcon());
            }
        });
        //获取是否有ai生成评论
        String key = RedisConstants.CACHE_AI_COMMENT_KEY + comment.getSourceType() + ":" + comment.getSourceId();
        String JsonStr = redisService.getCacheObject(key);
        if (JsonStr != null) {
            Comment commentDTO = JSON.parseObject(JsonStr, Comment.class);
            list.add(commentDTO);
        }
        return list;
    }

    /**
     * 获取子评论列表
     *
     * @param comment
     * @param current
     * @return
     */
    @Override
    public List<Comment> listChildComment(Comment comment, Integer current) {
        List<Comment> commentList = query()
                .eq("answer_id", comment.getId())
                .ne("status", "2")
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE))
                .getRecords();
        if(commentList!=null&& !commentList.isEmpty()){
            commentList.stream().forEach(c -> {
                Like like = new Like();
                like.setSourceType(GlobalBizTypeEnum.COMMENT.getCode());
                like.setSourceId(c.getId());
                c.setIsLike(iLikeService.isLike(like));
                Long id = c.getUserId();
                UserDTO user = remoteAppUserService.queryUserById(id);
                if (user != null) {
                    c.setNickName(user.getNickName());
                    c.setUserIcon(user.getIcon());
                }
            });
        }
        return commentList;
    }

    /**
     * 新增评论
     *
     * @param comment
     * @return
     */
    @Override
    @Transactional
    public Integer addComment(Comment comment) {
        comment.setCreateTime(DateUtils.getNowDate());
        int i = commentMapper.insertComment(comment);
        if (i > 0) {
            CommentTypeEnum commentType = CommentTypeEnum.getByCode(comment.getSourceType());
            String commentKeyPrefix = commentType.getCommentKeyPrefix()+ comment.getSourceId();
            String commentCountKeyPrefix = commentType.getCommentCountKeyPrefix()+ comment.getSourceId();
            String commentDirtyKeyPrefix = commentType.getCommentDirtyKeyPrefix();
            //保存评论信息
            redisService.setCacheZSet(commentKeyPrefix, comment.getId().toString(), System.currentTimeMillis());
            //记录评论数量
            redisService.incrementCacheValue(commentCountKeyPrefix);
            //记录脏数据
            redisService.setCacheSet(commentDirtyKeyPrefix, Collections.singleton(comment.getSourceId().toString()));
            //发送审核消息
            sendAuditMessage(comment);
        }
        return i;
    }
    /**
     * 发送审核消息
     * @param comment
     */
    private void sendAuditMessage(Comment comment) {
        ResourceStrategy resourceType = resourceStrategyFactory.getStrategy(comment.getSourceType());
        HashMap<String, String> content = resourceType.getResourceContentById(comment.getSourceId());
        AuditCommentBO auditCommentBO=new AuditCommentBO();
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
        MqMessageSendUtils.sendMqMessage(rabbitTemplate, MqConstants.AUDIT_EXCHANGE_NAME,MqConstants.AUDIT_ROUTING_KEY, auditMessage);
    }
    /**
     * 删除评论
     *
     * @param comment
     * @return
     */
    @Override
    @Transactional
    public Boolean deleteComment(Comment comment) {
        boolean i = removeById(comment.getId());
        if (i) {
            CommentTypeEnum commentType = CommentTypeEnum.getByCode(comment.getSourceType());
            String commentKeyPrefix = commentType.getCommentKeyPrefix()+ comment.getSourceId();
            String commentCountKeyPrefix = commentType.getCommentCountKeyPrefix()+ comment.getSourceId();
            String commentDirtyKeyPrefix = commentType.getCommentDirtyKeyPrefix();
            //删除评论信息
            redisService.removeCacheZSetObject(commentKeyPrefix, comment.getId().toString());
            //记录评论数量
            redisService.decrementCacheValue(commentCountKeyPrefix);
            //记录脏数据
            redisService.setCacheSet(commentDirtyKeyPrefix, Collections.singleton(comment.getId().toString()));
        }
        return i;
    }

    /**
     * 获取用户的评论
     *
     * @param current
     * @return
     */
    @Override
    public List<Comment> getCommentOfUser(Comment comment,Integer current) {
        Page<Comment> page = query()
                .eq("source_type", comment.getSourceType())
                .eq("parent_id", 0)
                .eq("user_id", comment.getUserId())
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Comment> list = page.getRecords();
        list.stream().forEach(c -> {
            ShopDTO shop = remoteShopService.getShopById(c.getSourceId());
            if (shop != null) {
                c.setSourceName(shop.getName());
            }
        });
        return list;
    }

    /**
     * 获取所有评论列表
     *
     * @return
     */
    @Override
    public List<Comment> getCommentList() {
        List<Comment> list = query().list();
        list.stream().forEach(c -> {
            if (c.getSourceType() == 1) {
                BlogDTO blog = remoteBlogService.getBlogById(c.getSourceId());
                if ((blog.getTitle() != null))
                    c.setSourceName(blog.getTitle());
            } else if (c.getSourceType() == 2) {
                ShopDTO shop = remoteShopService.getShopById(c.getSourceId());
                if ((shop!= null))
                    c.setSourceName(shop.getName());
            }
        });
        return list;
    }

    /**
     * 保存ai自动创建的评论
     *
     * @param comments
     * @return
     */
    @Override
    public Boolean saveAiCreateComment(List<Comment> comments) {
        if (comments.size() == 0) {
            throw new RuntimeException("请传入数据");
        }
        //清空redis缓存
        redisService.deleteObject(redisService.keys(RedisConstants.CACHE_AI_COMMENT_KEY + "*"));
        comments.forEach(commentDTO -> {
            String key = RedisConstants.CACHE_AI_COMMENT_KEY + commentDTO.getSourceType() + ":" + commentDTO.getSourceId();
            redisService.setCacheObject(key, JSON.toJSONString(commentDTO));
            //设置过期时间
//          redisService.expire(key, RedisConstants.CACHE_AI_COMMENT_TTL, TimeUnit.MINUTES);
        });
        return true;
    }

    /**
     * 获取用户发表的评论数
     *
     * @param comment
     * @return
     */
    @Override
    public Integer getCommentCount(Comment comment) {
        int commentCount = query()
                .eq(comment.getSourceType() != null, "source_type", comment.getSourceType())
                .eq(comment.getSourceId() != null, "source_id", comment.getSourceId())
                .eq(comment.getUserId() != null, "user_id", comment.getUserId())
                .count().intValue();
        return commentCount;
    }

    /**
     * 获取评论总数
     *
     * @return
     */
    @Override
    public Integer getCommentTotal() {
        return query().count().intValue();
    }

    /**
     * 创建ai自动创建的评论
     *
     * @param
     * @return
     */
    @Override
    public void aiCreateComment() {
        // 获取有评论的博客和店铺的id（保持顺序的去重）
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
        //发送rabbitMq消息给ai服务
        rabbitTemplate.convertAndSend(MqConstants.AI_EXCHANGE_NAME, MqConstants.AI_COMMENT_ROUTING, list);
    }

    /**
     * 根据评论id列表获取评论列表
     *
     * @param sourceIdList
     * @return
     */
    @Override
    public List<Comment> getCommentListByIds(List<Long> sourceIdList) {
        List<Comment> list = query().in("id", sourceIdList).list();
        return list;
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
        return true;
    }

    /**
     * 获取评论点赞数
     *
     * @param sourceId
     * @return
     */
    @Override
    public Integer getCommentLikeCount(Long sourceId) {
        Comment comment = getById(sourceId);
        return comment.getLiked();
    }

    /**
     * 根据id获取评论详情
     *
     * @param id
     * @return
     */
    @Override
    public Comment getCommentById(Long id) {
        Comment comment = getById(id);
        if (comment != null) {
            ShopDTO shop = remoteShopService.getShopById(comment.getSourceId());
            if(shop!=null){
                comment.setSourceName(shop.getName());
                comment.setShopImages(shop.getImages());
            }
            UserDTO userDTO = remoteAppUserService.queryUserById(comment.getUserId());
            if(userDTO!=null){
                comment.setNickName(userDTO.getNickName());
                comment.setUserIcon(userDTO.getIcon());
            }
        }
        return comment;
    }
    /**
     * 保存点赞用户列表到Redis
     *
     * @param key
      * @param commentList
     */
    private void saveCommentListToRedis(String key,List<Comment> commentList) {
        log.info("保存点赞用户列表到Redis{}",commentList);
        Set<ZSetOperations.TypedTuple<String>> followIdListSet = commentList.stream()
                .map(t -> {
                    ZSetOperations.TypedTuple<String> tuple = new DefaultTypedTuple<>(t.getId().toString(), (double) t.getCreateTime().getTime());
                    return tuple;
                })
                .collect(Collectors.toSet());
        redisService.setCacheZSet(key, followIdListSet);
    }

    /**
     * 更新评论状态
     * @param id 评论ID
     * @param status 状态
     * @return
     */
    @Override
    public Boolean updateCommentStatus(Long id, Integer status) {
        boolean update = update(new UpdateWrapper<Comment>()
                .set("status", status)
                .eq("id", id));
        if (update&&status== AuditStatusEnum.REJECT.getCode()) {
            //修改源数据的评论数量
            Comment comment = getById(id);
            CommentTypeEnum commentType = CommentTypeEnum.getByCode(comment.getSourceType());
            String commentKeyPrefix = commentType.getCommentKeyPrefix()+ comment.getSourceId();
            String commentCountKeyPrefix = commentType.getCommentCountKeyPrefix()+ comment.getSourceId();
            String commentDirtyKeyPrefix = commentType.getCommentDirtyKeyPrefix();
            //删除评论信息
            redisService.removeCacheZSetObject(commentKeyPrefix, comment.getId().toString());
            //记录评论数量
            redisService.decrementCacheValue(commentCountKeyPrefix);
            //记录脏数据
            redisService.setCacheSet(commentDirtyKeyPrefix, Collections.singleton(comment.getId().toString()));
        }
        return update;
    }
}
