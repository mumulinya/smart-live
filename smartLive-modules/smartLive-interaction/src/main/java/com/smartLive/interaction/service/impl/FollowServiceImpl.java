package com.smartLive.interaction.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.chat.api.dto.SystemNoticeCreateDTO;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.constant.mq.ChatMqConstants;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.enums.FeedTypeEnum;
import com.smartLive.common.core.enums.FollowTypeEnum;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.ItemActionType;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.rabbitmq.domain.FeedEventMessage;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.common.redis.util.ZSetIdManager;
import com.smartLive.interaction.domain.DTO.FollowDTO;
import com.smartLive.interaction.domain.Follow;
import com.smartLive.interaction.mapper.FollowMapper;
import com.smartLive.interaction.service.IFollowService;
import com.smartLive.interaction.strategy.factory.FollowStrategyFactory;
import com.smartLive.interaction.strategy.factory.ResourceStrategyFactory;
import com.smartLive.interaction.strategy.follow.FollowStrategy;
import com.smartLive.interaction.strategy.resource.ResourceStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 关注Service业务层处理
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Service
@Slf4j
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService
{
    @Autowired
    private FollowMapper followMapper;
    /**
     * 策略模式
     */
    @Autowired
    private ResourceStrategyFactory resourceStrategyFactory;
    /**
     * 策略模式
     */
    @Autowired
    private FollowStrategyFactory followStrategyFactory;
    @Autowired
    private ZSetIdManager zSetIdManager;
    @Autowired
    private RedisService redisService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    /**
     * 查询关注
     * 
     * @param id 关注主键
     * @return 关注
     */
    @Override
    public Follow selectFollowUserById(Long id)
    {
        return followMapper.selectFollowUserById(id);
    }

    /**
     * 查询关注列表
     * 
     * @param follow 关注
     * @return 关注
     */
    @Override
    public List<Follow> selectFollowUserList(Follow follow)
    {
        return followMapper.selectFollowUserList(follow);
    }

    /**
     * 新增关注
     * 
     * @param follow 关注
     * @return 结果
     */
    @Override
    public int insertFollowUser(Follow follow)
    {
        follow.setCreateTime(DateUtils.getNowDate());
        return followMapper.insertFollowUser(follow);
    }

    /**
     * 修改关注
     * 
     * @param follow 关注
     * @return 结果
     */
    @Override
    public int updateFollowUser(Follow follow)
    {
        return followMapper.updateFollowUser(follow);
    }

    /**
     * 批量删除关注
     * 
     * @param ids 需要删除的关注主键
     * @return 结果
     */
    @Override
    public int deleteFollowUserByIds(Long[] ids)
    {
        return followMapper.deleteFollowUserByIds(ids);
    }

    /**
     * 删除关注信息
     * 
     * @param id 关注主键
     * @return 结果
     */
    @Override
    public int deleteFollowUserById(Long id)
    {
        return followMapper.deleteFollowUserById(id);
    }


    /**
     * 关注或取关
     *
     * @param
     * @param
     * @return
     */
    @Override
    public Boolean follow(Follow follow) {
        if(UserContextHolder.getUser()==null){
            return false;
        }
        //获取当前用户id
        Long userId = UserContextHolder.getUser().getId();
        // 1. 获取对应的枚举策略
        FollowTypeEnum followType = FollowTypeEnum.getByCode(follow.getSourceType());
        if (followType == null) {
            return false;
        }
        //2.我的关注列表
        String myFollowKey = followType.getFollowKeyPrefix() + userId;

        // 3. 对方的粉丝列表
        String targetFansKey = followType.getFansKeyPrefix() + follow.getSourceId();
        String followDirtyKey = followType.getFollowDirtyKeyPrefix();
        String fansDirtyKey = followType.getFansDirtyKeyPrefix();
        // 4. 独立计数器 key
        String followCountKey = followType.getFollowCountKeyPrefix() + userId;
        String fansCountKey = followType.getFansCountKeyPrefix() + follow.getSourceId();
        //判断是关注还是取关
        if(Boolean.TRUE.equals(follow.getIsFollow())){
            //关注
            follow.setUserId(userId);
            follow.setCreateTime(DateUtils.getNowDate());
            boolean save = save(follow);
            if (save) {
                //关注成功，添加关注到redis ZSet
                redisService.setCacheZSet(myFollowKey, follow.getSourceId().toString(), System.currentTimeMillis());
                redisService.setCacheZSet(targetFansKey, userId.toString(), System.currentTimeMillis());
                //标记脏数据
                redisService.setCacheSet(followDirtyKey, userId.toString());
                redisService.setCacheSet(fansDirtyKey, follow.getSourceId().toString());
                //确保 Redis 计数器已初始化，再递增独立计数器
                getFollowCount(follow);
                getFanCount(follow);
                redisService.incrementCacheValue(followCountKey);
                redisService.incrementCacheValue(fansCountKey);
                //同步个人资源到es
                followStrategyFactory.getStrategy(follow.getSourceType()).syncUserResource(userId, follow.getSourceId());
            }
            return save;
        }else{
            //取关
            boolean remove = remove(new QueryWrapper<Follow>().eq("user_id", userId).eq("source_type",follow.getSourceType()).eq("source_id", follow.getSourceId()));
            if (remove) {
                //取关成功，从redis ZSet中删除
                redisService.removeCacheZSetObject(myFollowKey, follow.getSourceId().toString());
                redisService.removeCacheZSetObject(targetFansKey, userId.toString());
                //标记脏数据
                redisService.setCacheSet(followDirtyKey, userId.toString());
                redisService.setCacheSet(fansDirtyKey, follow.getSourceId().toString());
                //确保 Redis 计数器已初始化，再递减独立计数器
                getFollowCount(follow);
                getFanCount(follow);
                redisService.decrementCacheValue(followCountKey);
                redisService.decrementCacheValue(fansCountKey);
            }
            return remove;
        }
    }

    /**
     * 判断是否关注
     *
     * @param follow
     * @return
     */
    @Override
    public Boolean isFollowed(Follow follow) {
        com.smartLive.common.core.domain.LoginUser user = UserContextHolder.getUser();
        if (user == null) {
            return false;
        }
        //获取当前用户id
        Long userId = user.getId();
        // 1. 获取对应的枚举策略
        FollowTypeEnum followType = FollowTypeEnum.getByCode(follow.getSourceType());
        if (followType == null) {
            log.error("关注类型错误");
            return false;
        }
        String key =followType.getFollowKeyPrefix()+userId;

        //判断是否关注 从redis的zSet集合中查询
        //如果分数不为 null，说明元素存在（已关注）；如果为 null，说明不存在（未关注）
        Boolean isFollow =redisService.getCacheZSetScore(key, follow.getSourceId().toString())!= null;
       if(isFollow){
           //已关注
           return true;
       }
       //判断是否关注 从数据库中查询
        Integer count = query()
                .eq("source_type", follow.getSourceType())
                .eq("user_id", userId)
                .eq("source_id", follow.getSourceId())
                .count().intValue();
       return count > 0;
    }

    /**
     * 共同关注 userId
     *
     * @param
     * @return
     */
    @Override
    public List<?> common(Follow follow, Integer current) {
        FollowTypeEnum followTypeEnum = FollowTypeEnum.getByCode(follow.getSourceType());
        if (followTypeEnum == null) {
            log.error("关注类型错误");
            return Collections.emptyList();
        }
        //获取当前用户id
        Long currentUserId = UserContextHolder.getUser().getId();
        //从redis读取列表
        Page<Long> commonFollowPage =  zSetIdManager.pageCommonFollowIds(followTypeEnum.getFollowKeyPrefix(), currentUserId, follow.getUserId(), current, SystemConstants.MAX_PAGE_SIZE);
        List<Long> idList = commonFollowPage.getRecords();
        //如果redis里面没有数据，则从数据库里面获取
        if (commonFollowPage.getTotal()==0) {
            //获取当前用户的关注列表
            List<Follow> currentUserFollowList = query()
                    .eq("source_type", follow.getSourceType())
                    .eq("user_id", currentUserId)
                    .list();
            //获取当前用户的粉丝id列表
            List<Long> currentUserFollowIdList = new ArrayList<>();
            if (!currentUserFollowList.isEmpty()) {
                currentUserFollowIdList = currentUserFollowList.stream().map(Follow::getSourceId).collect(Collectors.toList());
                //把当前用户关注列表id写入redis当中
                zSetIdManager.saveToZSet(followTypeEnum.getFollowKeyPrefix()+currentUserId, currentUserFollowList, Follow::getSourceId, Follow::getCreateTime);
            }
            //获取目标用户的关注列表
            List<Follow> targetUserFollowList = query()
                    .eq("source_type", follow.getSourceType())
                    .eq("user_id", follow.getUserId())
                    .list();
            //获取目标用户的粉丝id列表
            List<Long> targetUserFollowIdList = new ArrayList<>();
            if (!targetUserFollowList.isEmpty()) {
                targetUserFollowIdList = targetUserFollowList.stream().map(Follow::getSourceId).collect(Collectors.toList());
                //把目标用户关注列表id写入redis当中
                zSetIdManager.saveToZSet(followTypeEnum.getFollowKeyPrefix()+follow.getUserId(), targetUserFollowList, Follow::getSourceId, Follow::getCreateTime);

            }
            //获取两个列表的交集
          List<Long> commonFollowList = currentUserFollowIdList.stream()
                    .filter(targetUserFollowIdList::contains)
                    .collect(Collectors.toList());
          if(commonFollowList.isEmpty()){
              return Collections.emptyList();
          }
          if(!commonFollowList.isEmpty()){
              //截取当前页
              int start = Math.max(0, (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE);
              if (start < commonFollowList.size()) {
                  int end = Math.min(commonFollowList.size(), start + SystemConstants.DEFAULT_PAGE_SIZE);
                  idList = commonFollowList.subList(start, end);
              } else {
                  idList = Collections.emptyList();
              }
          }
        }
        if(idList==null||idList.isEmpty()){
            return Collections.emptyList();
        }
//        IdentityStrategy identityStrategy = identityStrategyMap.get(followTypeEnum.getCode());
//        List<?> socialInfoVOList = identityStrategy.getFollowList(idList);
        ResourceStrategy resourceStrategy = resourceStrategyFactory.getStrategy(followTypeEnum.getCode());
        List<?> socialInfoVOList = resourceStrategy.getResourceList(idList);
        return socialInfoVOList;
    }

    /**
     * 推送数据给粉丝
     *
     * @param feedEventMessage
     */
    @Override
    public void pushToFollowers(FeedEventMessage feedEventMessage) {
        //从redis里面读取粉丝列表
        FollowTypeEnum followType = FollowTypeEnum.getByCode(feedEventMessage.getSourceType());
        if (followType == null) {
            log.error("推送数据给粉丝失败，未知的关注类型");
            return;
        }
        String fansKey = followType.getFansKeyPrefix() + feedEventMessage.getSourceId();
        Set<String> userIdSet= redisService.getCacheZSetRange(fansKey, 0, -1);
        List<Long> userIdList = userIdSet.stream().map(Long::valueOf).collect(Collectors.toList());
        //推送笔记id给所有粉丝
        // 查询笔记作者下的所有粉丝
        if(userIdList.isEmpty()){
            userIdList = query().select("user_id").eq("source_type", feedEventMessage.getSourceType()).eq("source_id", feedEventMessage.getSourceId()).list().stream().map(Follow::getUserId).collect(Collectors.toList());
        }
        if(userIdList.isEmpty()){
            log.info("推送数据给粉丝失败，没有粉丝");
            return;
        }
        //获取发送数据源的类型
        String bizDomain = GlobalBizTypeEnum.getByCode(feedEventMessage.getBizType()).getBizDomain();
        String feedKeyPrefix = FeedTypeEnum.getByCode(feedEventMessage.getFeedType()).getFeedKeyPrefix();
        String value = bizDomain+":"+feedEventMessage.getBizId().toString();
        if(feedEventMessage.getAction()!=null){
            value=feedEventMessage.getAction()+":"+value;
        }
        boolean isSendSystemNotice=false;
        if(feedEventMessage.getBizType()==GlobalBizTypeEnum.PRODUCT.getCode()){
            isSendSystemNotice=true;
        }
        // 优化方案：采用 Pipeline 批量写入替代循环写入
        List<String> userFeedKeys = new ArrayList<>(userIdList.size());
        List<String> allFeedKeys = new ArrayList<>(userIdList.size());
        
        for (Long userId : userIdList) {
            userFeedKeys.add(feedKeyPrefix + userId);
            allFeedKeys.add(FeedTypeEnum.ALL_FEED.getFeedKeyPrefix() + userId);
            
            if(isSendSystemNotice){
                // 发送系统通知（当前设计为RPC，可进一步考虑发MQ或本地事件机制）
                createSystemNotice(userId, feedEventMessage);
            }
        }
        
        long currentTimeMillis = System.currentTimeMillis();
        // Pipeline 批量写入个人动态 Feed 中继层
        redisService.setCacheZSetBatch(userFeedKeys, value, currentTimeMillis);
        // Pipeline 批量写入总 Feed 聚合层
        redisService.setCacheZSetBatch(allFeedKeys, value, currentTimeMillis);
    }
    /**
     * 创建系统通知
     *
     * @param userId
     * @param feedEventMessage
     */
    private void createSystemNotice(Long userId, FeedEventMessage feedEventMessage) {
        ResourceStrategy resourceStrategy = resourceStrategyFactory.getStrategy(feedEventMessage.getSourceType());
        Object resource = resourceStrategy.getResourceById(feedEventMessage.getSourceId());
        HashMap<String,String> hashMap = resourceStrategy.getResourceContentById(feedEventMessage.getSourceId());
        //使用方法转化为map
        Map<String, Object> map = BeanUtil.beanToMap(resource);
        if (userId == null) {
            return;
        }
        try {
            GlobalBizTypeEnum globalBizTypeEnum = GlobalBizTypeEnum.getByCode(feedEventMessage.getBizType());
            String desc = ItemActionType.getDescByCode(feedEventMessage.getAction());
            String content = "你关注的"+ globalBizTypeEnum.getDesc()+hashMap.get("title")+ desc;
            SystemNoticeCreateDTO createDTO = new SystemNoticeCreateDTO();
            createDTO.setUserId(userId);
            createDTO.setSourceType(feedEventMessage.getSourceType());
            createDTO.setSourceId(feedEventMessage.getSourceId());
            createDTO.setAction(globalBizTypeEnum.getBizDomain()+"_"+feedEventMessage.getAction());
            createDTO.setContent(content);
            createDTO.setTitle(globalBizTypeEnum.getDesc()+desc);
            createDTO.setExtraData(map);
            // 通过 MQ 异步发送系统通知，解耦 interaction 模块与 chat 模块，避免同步 RPC 流量放大
            MqMessageSendUtils.sendMqMessage(rabbitTemplate,
                    ChatMqConstants.SYSTEM_NOTICE_EXCHANGE,
                    ChatMqConstants.SYSTEM_NOTICE_ROUTING,
                    createDTO);
        } catch (Exception e) {
            log.error("create reject system notice failed, auditTaskId={}", feedEventMessage.getBizId(), e);
        }
    }
    /**
     * 获取粉丝列表
     *
     * @return
     */
    @Override
    public List<?> getFans(Follow follow,Integer current) {
        // 1. 获取对应的枚举策略
        FollowTypeEnum identityType = FollowTypeEnum.getByCode(follow.getSourceType());
        if (identityType == null) {
            log.error("关注类型错误");
            return Collections.emptyList();
        }
        //从redis获取
        Page<Long> fanIdPage = zSetIdManager.pageIds(identityType.getFansKeyPrefix(), follow.getSourceId(),current, SystemConstants.DEFAULT_PAGE_SIZE);
        List<Long> sourceIdList = fanIdPage.getRecords();
        //redis获取失败，从数据库获取
        if (sourceIdList.isEmpty()) {
            //获取粉丝id
            List<Follow> sourceList = query()
                    .eq("source_type",follow.getSourceType())
                    .eq("source_id", follow.getSourceId())
                    .orderByDesc("create_time") // 添加排序
                    .list();
            if(!sourceList.isEmpty()){
                sourceIdList = sourceList.stream().map(Follow::getUserId).collect(Collectors.toList());
                //截取
                if(!sourceIdList.isEmpty()){
                    //截取当前页
                    int start = Math.max(0, (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE);
                    if (start < sourceIdList.size()) {
                        int end = Math.min(sourceIdList.size(), start + SystemConstants.DEFAULT_PAGE_SIZE);
                        sourceIdList = sourceIdList.subList(start, end);
                    } else {
                        sourceIdList = Collections.emptyList();
                    }
                }
                //存入redis
                zSetIdManager.saveToZSet(identityType.getFansKeyPrefix()+follow.getSourceId(), sourceList, Follow::getUserId, Follow::getCreateTime);

            }
        }
        //根据id查询用户
       if(sourceIdList.isEmpty()){
           return Collections.emptyList();
       }
        //根据关注类型从关注策略工程获取bean
        ResourceStrategy resourceStrategy = resourceStrategyFactory.getStrategy(identityType.getCode());
        List<?> socialInfoVOList = resourceStrategy.getResourceList(sourceIdList);
        return socialInfoVOList;
    }

    /**
     * 获取关注列表
     *
     * @param followDTO
     * @return
     */
    @Override
    public List<?> getFollows(FollowDTO followDTO, Integer current) {
//        // 1. 获取对应的枚举策略
        FollowTypeEnum followTypeEnum = FollowTypeEnum.getByCode(followDTO.getSourceType());
        if (followTypeEnum == null) {
            log.error("关注类型错误");
            return Collections.emptyList();
        }
        //从redis获取
        Page<Long> fanIdPage = zSetIdManager.pageIds(followTypeEnum.getFollowKeyPrefix(), followDTO.getUserId(),current, SystemConstants.DEFAULT_PAGE_SIZE);
        List<Long> sourceIdList = fanIdPage.getRecords();
        //redis获取失败，从数据库获取
        if (sourceIdList.isEmpty()) {
            //获取粉丝id
            List<Follow> sourceList = query()
                    .eq("source_type",followDTO.getSourceType())
                    .eq("user_id", followDTO.getUserId())
                    .orderByDesc("create_time") // 添加排序
                    .list();
            if(!sourceList.isEmpty()){
                sourceIdList = sourceList.stream().map(Follow::getSourceId).collect(Collectors.toList());
                //截取
                if(!sourceIdList.isEmpty()){
                    //截取当前页
                    int start = Math.max(0, (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE);
                    if (start < sourceIdList.size()) {
                        int end = Math.min(sourceIdList.size(), start + SystemConstants.DEFAULT_PAGE_SIZE);
                        sourceIdList = sourceIdList.subList(start, end);
                    } else {
                        sourceIdList = Collections.emptyList();
                    }
                }
                //存入redis
                zSetIdManager.saveToZSet(followTypeEnum.getFollowKeyPrefix()+followDTO.getUserId(), sourceList, Follow::getSourceId, Follow::getCreateTime);
            }
           }
        if(sourceIdList.isEmpty()){
            return Collections.emptyList();
        }
        //根据关注类型从关注策略工程获取bean
        ResourceStrategy resourceStrategy = resourceStrategyFactory.getStrategy(followTypeEnum.getCode());
        List<?> socialInfoVOList = resourceStrategy.getResourceList(sourceIdList);
        return socialInfoVOList;
    }

    /**
     * 获取关注数
     * 优先级: Redis 独立计数器 → 源数据表 → follow 表 COUNT
     *
     * @param follow 关注查询条件
     * @return 关注数量
     */
    @Override
    public Integer getFollowCount(Follow follow) {
        FollowTypeEnum followType = FollowTypeEnum.getByCode(follow.getSourceType());
        // 1. 从 Redis 独立计数器读取关注数
        String followCountKey = followType.getFollowCountKeyPrefix() + follow.getUserId();
        Integer followCount = redisService.getCacheObject(followCountKey);
        if (followCount == null) {
            // 2. 计数器不存在，先从源数据表查询（如 user_info.followee）
            FollowStrategy strategy = followStrategyFactory.getStrategy(follow.getSourceType());
            followCount = strategy.getFollowCount(follow.getUserId());
            // 3. 源表也查不到，fallback 到 follow 表 COUNT
            if (followCount == null) {
                followCount = query().eq("source_type", follow.getSourceType()).eq("user_id", follow.getUserId()).count().intValue();
            }
            // 回写 Redis 缓存
            redisService.setCacheObject(followCountKey, followCount);
        }
        return followCount;
    }

    /**
     * 获取粉丝数
     * 优先级: Redis 独立计数器 → 源数据表 → follow 表 COUNT
     *
     * @param follow 关注查询条件
     * @return 粉丝数量
     */
    @Override
    public Integer getFanCount(Follow follow) {
        FollowTypeEnum followType = FollowTypeEnum.getByCode(follow.getSourceType());
        // 1. 从 Redis 独立计数器读取粉丝数
        String fansCountKey = followType.getFansCountKeyPrefix() + follow.getSourceId();
        Integer fansCount = redisService.getCacheObject(fansCountKey);
        if (fansCount == null) {
            // 2. 计数器不存在，先从源数据表查询（如 user_info.fans、shop.fans）
            FollowStrategy strategy = followStrategyFactory.getStrategy(follow.getSourceType());
            fansCount = strategy.getFanCount(follow.getSourceId());
            // 3. 源表也查不到，fallback 到 follow 表 COUNT
            if (fansCount == null) {
                fansCount = query().eq("source_type", follow.getSourceType()).eq("source_id", follow.getSourceId()).count().intValue();
            }
            // 回写 Redis 缓存
            redisService.setCacheObject(fansCountKey, fansCount);
        }
        return fansCount;
    }

    /**
     * 获取共同关注数
     *
     * @param follow
     * @return
     */
    @Override
    public Integer getCommonFollowCount(Follow follow) {
        log.info("获取共同关注数：{}",follow);
        FollowTypeEnum followTypeEnum = FollowTypeEnum.getByCode(follow.getSourceType());
        if (followTypeEnum == null) {
            log.error("关注类型错误");
            return 0;
        }
        Long currentUserId=follow.getUserId();
        //从redis读取
        Page<Long> commonFollowIdPage = zSetIdManager.pageCommonFollowIds(followTypeEnum.getFollowKeyPrefix(), follow.getSourceId(), currentUserId,1, 0);
        int commonFollowCount = (int) commonFollowIdPage.getTotal();
        log.info("从redis获取共同关注数：{}",commonFollowCount);
        //redis获取失败，从数据库获取
        if(commonFollowCount==0){
            // 提取关注用户ID列表
            List<Long> targetUserFollowIds = lambdaQuery()
                    .select(Follow::getSourceId)
                    .eq(Follow::getSourceType, follow.getSourceType())
                    .eq(Follow::getUserId, follow.getSourceId())
                    .list()
                    .stream()
                    .map(Follow::getSourceId)
                    .collect(Collectors.toList());
            // 如果目标用户没有关注任何人，直接返回0
            if (targetUserFollowIds.isEmpty()) {
                commonFollowCount = 0;
            } else {
                // 查询共同关注数
                commonFollowCount = lambdaQuery()
                        .select(Follow::getSourceId)
                        .eq(Follow::getSourceType, follow.getSourceType())
                        .eq(Follow::getUserId, currentUserId)
                        .in(Follow::getSourceId, targetUserFollowIds)
                        .count()
                        .intValue();
            }
        }
        return commonFollowCount;
    }
}
