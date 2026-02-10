package com.smartLive.interaction.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.ReviewTypeEnum;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.rabbitmq.domain.AuditMessage;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.interaction.domain.BO.AuditCommentBO;
import com.smartLive.interaction.domain.BO.AuditReviewBO;
import com.smartLive.interaction.domain.Comment;
import com.smartLive.interaction.domain.Like;
import com.smartLive.interaction.domain.Review;
import com.smartLive.interaction.domain.Star;
import com.smartLive.interaction.mapper.ReviewMapper;
import com.smartLive.interaction.service.ILikeService;
import com.smartLive.interaction.service.IReviewService;
import com.smartLive.interaction.service.IStarService;
import com.smartLive.interaction.strategy.resource.ResourceStrategy;
import com.smartLive.interaction.tool.QueryRedisSourceIdsTool;
import com.smartLive.order.api.RemoteOrderService;
import com.smartLive.shop.api.DTO.ShopDTO;
import com.smartLive.shop.api.RemoteShopService;
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
    private QueryRedisSourceIdsTool queryRedisSourceIdsTool;
    @Autowired
    private RedisService redisService;

    private ILikeService likeService;
    private IStarService starService;
    @Autowired
    private RemoteOrderService remoteOrderService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    private Map<Integer, ResourceStrategy> resourceStrategyMap;

    @Autowired
    public ReviewServiceImpl(@Lazy ILikeService likeService, @Lazy IStarService starService,@Lazy Map<Integer, ResourceStrategy> resourceStrategyMap) {
        this.likeService = likeService;
        this.starService = starService;
        this.resourceStrategyMap = resourceStrategyMap;
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
        return reviewMapper.deleteReviewByIds(ids);
    }

    /**
     * 删除评论信息
     *
     * @param id 评论主键
     * @return 结果
     */
    @Override
    public int deleteReviewById(Long id) {
        return reviewMapper.deleteReviewById(id);
    }

    /**
     * 获取评价列表
     *
     * @param review
     * @return
     */
    @Override
    public List<Review> listReview(Review review, Integer current) {
        //从redis里面获取
        ReviewTypeEnum reviewType = ReviewTypeEnum.getByCode(review.getSourceType());
        if (reviewType == null) {
            log.error("参数错误");
            return Collections.emptyList();
        }
        String reviewKeyPrefix = reviewType.getReviewKeyPrefix();
        Page<Long> longPage = queryRedisSourceIdsTool.queryRedisIdPage(reviewKeyPrefix, review.getSourceId(), current, SystemConstants.MAX_PAGE_SIZE);
        List<Long> reviewIdList = longPage.getRecords();
        List<Review> list=new ArrayList<>();
        if(reviewIdList != null && reviewIdList.size() > 0){
             list = lambdaQuery().in(Review::getId, reviewIdList).orderByDesc(Review::getLiked).list();
        }
        //如果redis里面没有数据，则从数据库里面获取
        if(list == null || list.size() == 0){
            log.info("从数据库里面获取");
             list = query()
                    .eq("source_id", review.getSourceId())
                     .eq("status", 0)
                    .eq("source_type", review.getSourceType())
                    .orderByDesc("liked")
                    .list();
            //截取
            if(!list.isEmpty()){
                saveReviewListToRedis(reviewKeyPrefix+review.getSourceId(), list);
                //截取当前页
                list = list.size() > SystemConstants.DEFAULT_PAGE_SIZE ? list.subList((current-1)*SystemConstants.DEFAULT_PAGE_SIZE, (current-1)*SystemConstants.DEFAULT_PAGE_SIZE + SystemConstants.DEFAULT_PAGE_SIZE) : list;
            }
        }
        if(list == null){
            return Collections.emptyList();
        }
        list.stream().forEach(c -> {
            //判断是否点赞
            Like like = new Like();
            like.setSourceType(GlobalBizTypeEnum.REVIEW.getCode());
            like.setSourceId(c.getId());
            c.setIsLike(likeService.isLike(like));
            Star star=new Star();
            star.setSourceId(c.getId());
            star.setSourceType(GlobalBizTypeEnum.REVIEW.getCode());
            starService.isStar(star);
            c.setIsStared(starService.isStar(star));
            //判断是否收藏
            Long id = c.getUserId();
            UserDTO user = remoteAppUserService.queryUserById(id);
            if (user != null) {
                c.setNickName(user.getNickName());
                c.setUserIcon(user.getIcon());
            }
        });
        //获取是否有ai生成评论
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
            String reviewKeyPrefix = reviewType.getReviewKeyPrefix()+ review.getSourceId();
            String reviewCountKeyPrefix = reviewType.getReviewCountKeyPrefix()+ review.getSourceId();
            String reviewDirtyKeyPrefix = reviewType.getReviewDirtyKeyPrefix();
            //保存评价信息
            redisService.setCacheZSet(reviewKeyPrefix, review.getId().toString(), System.currentTimeMillis());
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
        ResourceStrategy resourceType = resourceStrategyMap.get(review.getSourceType());
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
        boolean i = removeById(review.getId());
        if (i) {
            ReviewTypeEnum reviewType = ReviewTypeEnum.getByCode(review.getSourceType());
            String reviewKeyPrefix = reviewType.getReviewKeyPrefix()+ review.getSourceId();
            String reviewCountKeyPrefix = reviewType.getReviewCountKeyPrefix()+ review.getSourceId();
            String reviewDirtyKeyPrefix = reviewType.getReviewDirtyKeyPrefix();
            //删除评论信息
            redisService.removeCacheZSetObject(reviewKeyPrefix, review.getId().toString());
            //记录评论数量
            redisService.decrementCacheValue(reviewCountKeyPrefix);
            //记录脏数据
            redisService.setCacheSet(reviewDirtyKeyPrefix, Collections.singleton(review.getId().toString()));
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
        Page<Review> page = query()
                .eq("user_id", review.getUserId())
                .eq("status", review.getStatus())
                .orderByDesc("create_time")
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Review> list = page.getRecords();
        list.stream().forEach(c -> {
            ShopDTO shop = remoteShopService.getShopById(c.getShopId());
            if (shop != null) {
                c.setSourceName(shop.getName());
            }
        });
        return list;
    }
    /**
     * 获取用户发表的评论数
     *
     * @param review
     * @return
     */
    @Override
    public Integer getReviewCount(Review review) {
        int reviewCount = query()
                .eq(review.getSourceType() != null, "source_type", review.getSourceType())
                .eq(review.getSourceId() != null, "source_id", review.getSourceId())
                .eq(review.getUserId() != null, "user_id", review.getUserId())
                .count().intValue();
        return reviewCount;
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
        List<Review> list = query().in("id", sourceIdList).list();
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
        Review review = getById(id);
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

    /**
     * 保存点赞用户列表到Redis
     *
     * @param key
      * @param reviewList
     */
    private void saveReviewListToRedis(String key,List<Review> reviewList) {
        log.info("保存点赞用户列表到Redis{}",reviewList);
        Set<ZSetOperations.TypedTuple<String>> followIdListSet = reviewList.stream()
                .map(t -> {
                    ZSetOperations.TypedTuple<String> tuple = new DefaultTypedTuple<>(t.getId().toString(), (double) t.getCreateTime().getTime());
                    return tuple;
                })
                .collect(Collectors.toSet());
        if (CollUtil.isNotEmpty(followIdListSet)) {
            redisService.setCacheZSet(key, followIdListSet);
        }
    }
}
