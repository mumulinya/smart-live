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
import com.smartLive.common.core.domain.AppLoginUser;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.enums.interaction.FeedTypeEnum;
import com.smartLive.common.core.enums.interaction.FollowTypeEnum;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.product.ItemActionType;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 关注关系业务实现层
 * 维系系统内用户与用户、用户与店铺之间的社交纽带。
 * 
 * 技术要点：
 * 1. 社交关系存储：利用 Redis ZSet 极速判断“是否关注”及“共同关注”。
 * 2. 脏数据处理：通过 Set 记录发生变动的实体，便于后续全量同步。
 * 3. 性能优化：采用 Pipeline 批量推送动态给粉丝，提升高并发下的响应速度。
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
    private MqMessageSendUtils mqMessageSendUtils;
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
     * 核心关注/取关逻辑
     * 1. 防止重复操作。
     * 2. 数据库事务保存记录。
     * 3. Redis 双向 ZSet 维护（我的关注、对方粉丝）。
     * 4. 实时更新关注/粉丝计数的缓存。
     * 5. 同步个人画像相关资源。
     */
    @Override
    public Boolean follow(FollowDTO followDTO) {
        if(UserContextHolder.getUser()==null){
            return false;
        }
        if(Boolean.TRUE.equals(followDTO.getIsFollow())){
            //先判断是否关注，防止重复关注
            Follow follow = new Follow();
            follow.setSourceType(followDTO.getSourceType());
            follow.setSourceId(followDTO.getSourceId());
            Boolean followed = isFollowed(follow);
            if (followed) {
                return true;
            }
        }
        //获取当前用户id
        Long userId = UserContextHolder.getUser().getId();
        // 1. 获取对应的枚举策略
        FollowTypeEnum followType = FollowTypeEnum.getByCode(followDTO.getSourceType());
        if (followType == null) {
            return false;
        }
        //2.我的关注列表
        String myFollowKey = followType.getFollowKeyPrefix() + userId;

        // 3. 对方的粉丝列表
        String targetFansKey = followType.getFansKeyPrefix() + followDTO.getSourceId();
        String followDirtyKey = followType.getFollowDirtyKeyPrefix();
        String fansDirtyKey = followType.getFansDirtyKeyPrefix();
        // 4. 独立计数器 key
        String followCountKey = followType.getFollowCountKeyPrefix() + userId;
        String fansCountKey = followType.getFansCountKeyPrefix() + followDTO.getSourceId();

        // 用于 getFollowCount / getFanCount 查询
        Follow countQuery = new Follow(followDTO.getSourceType(), followDTO.getSourceId());

        //判断是关注还是取关
        if(Boolean.TRUE.equals(followDTO.getIsFollow())){
            //关注
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setSourceType(followDTO.getSourceType());
            follow.setSourceId(followDTO.getSourceId());
            follow.setCreateTime(DateUtils.getNowDate());
            boolean save = save(follow);
            if (save) {
                //关注成功，添加关注到redis ZSet
                redisService.setCacheZSet(myFollowKey, followDTO.getSourceId().toString(), System.currentTimeMillis());
                redisService.setCacheZSet(targetFansKey, userId.toString(), System.currentTimeMillis());
                //标记脏数据
                redisService.setCacheSet(followDirtyKey, userId.toString());
                redisService.setCacheSet(fansDirtyKey, followDTO.getSourceId().toString());
                //确保 Redis 计数器已初始化，再递增独立计数器
                getFollowCount(countQuery);
                getFanCount(countQuery);
                redisService.incrementCacheValue(followCountKey);
                redisService.incrementCacheValue(fansCountKey);
                //同步个人资源到es
                followStrategyFactory.getStrategy(followDTO.getSourceType()).syncUserResource(userId, followDTO.getSourceId());
            }
            return save;
        }else{
            //取关
            boolean remove = remove(new QueryWrapper<Follow>().eq("user_id", userId).eq("source_type",followDTO.getSourceType()).eq("source_id", followDTO.getSourceId()));
            if (remove) {
                //取关成功，从redis ZSet中删除
                redisService.removeCacheZSetObject(myFollowKey, followDTO.getSourceId().toString());
                redisService.removeCacheZSetObject(targetFansKey, userId.toString());
                //标记脏数据
                redisService.setCacheSet(followDirtyKey, userId.toString());
                redisService.setCacheSet(fansDirtyKey, followDTO.getSourceId().toString());
                //确保 Redis 计数器已初始化，再递减独立计数器
                getFollowCount(countQuery);
                getFanCount(countQuery);
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
        AppLoginUser user = UserContextHolder.getUser();
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
     * 将发布的动态推送给粉丝（拉推结合模式中的推部分）
     * 流程：
     * 1. 获取目标实体的粉丝列表。
     * 2. 将动态信息（业务类型:ID）写入所有粉丝的 Feed 集合中。
     * 3. 针对特定业务类型（如新商品发布）触发实时系统消息通知。
     *
     * @param feedEventMessage 动态事件消息
     */
    @Override
    public void pushToFollowers(FeedEventMessage feedEventMessage) {
        //从redis里面读取粉丝列表
        FollowTypeEnum followType = FollowTypeEnum.getByCode(feedEventMessage.getSourceType());
        if (followType == null) {
            log.error("推送数据给粉丝失败，未知的关注类型");
            return;
        }
        //推送数据给所有粉丝
        String fansKey = followType.getFansKeyPrefix() + feedEventMessage.getSourceId();
        Set<String> userIdSet= redisService.getCacheZSetRange(fansKey, 0, -1);
        List<Long> userIdList = userIdSet.stream().map(Long::valueOf).collect(Collectors.toList());
        // 从数据库查询当前源目标下的所有粉丝
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
            //如果是店铺的事件的话，要把店铺id一起写入进去
            if(Objects.equals(feedEventMessage.getSourceType(), FeedTypeEnum.SHOP_FEED.getCode())){
                value=feedEventMessage.getSourceId().toString()+":"+value;
            }
        }
        //判断是否发送系统通知(商品有关的动态发送系统通知)
        boolean isSendSystemNotice= Objects.equals(feedEventMessage.getBizType(), GlobalBizTypeEnum.PRODUCT.getCode());
        // 优化方案：采用 Pipeline 批量写入替代循环写入
        List<String> userFeedKeys = new ArrayList<>(userIdList.size());
        List<String> allFeedKeys = new ArrayList<>(userIdList.size());
        
        for (Long userId : userIdList) {
            userFeedKeys.add(feedKeyPrefix + userId);
            allFeedKeys.add(FeedTypeEnum.ALL_FEED.getFeedKeyPrefix() + userId);
            
            if(isSendSystemNotice){
                // 发送系统通知(发mq消息)
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
        ResourceStrategy resourceStrategy = resourceStrategyFactory.getStrategy(feedEventMessage.getBizType());
        Object resource = resourceStrategy.getResourceById(feedEventMessage.getBizId());
        HashMap<String,String> hashMap = resourceStrategy.getResourceContentById(feedEventMessage.getBizId());
        //使用方法转化为map
        Map<String, Object> map = BeanUtil.beanToMap(resource);
        if (userId == null) {
            return;
        }
        try {
            GlobalBizTypeEnum globalBizTypeEnum = GlobalBizTypeEnum.getByCode(feedEventMessage.getSourceType());
            String desc = ItemActionType.getDescByCode(feedEventMessage.getAction());
            String content = "你关注的"+ globalBizTypeEnum.getDesc()+":《"+hashMap.get("title")+"》:"+ desc;
            SystemNoticeCreateDTO createDTO = new SystemNoticeCreateDTO();
            createDTO.setUserId(userId);
            createDTO.setSourceType(feedEventMessage.getBizType());
            createDTO.setSourceId(feedEventMessage.getBizId());
            createDTO.setAction(globalBizTypeEnum.getBizDomain()+"_"+feedEventMessage.getAction());
            createDTO.setContent(content);
            createDTO.setTitle(globalBizTypeEnum.getDesc()+desc);
            createDTO.setExtraData(map);
            // 通过 MQ 异步发送系统通知，解耦 interaction 模块与 chat 模块，避免同步 RPC 流量放大
            mqMessageSendUtils.sendMqMessage(
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
            if (followCount == null||followCount == 0) {
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
            if (fansCount == null||fansCount == 0) {
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
