package com.smartLive.interaction.service.impl;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.enums.FeedTypeEnum;
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.IdentityTypeEnum;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.api.dto.FeedEventDTO;
import com.smartLive.interaction.domain.Follow;
import com.smartLive.interaction.domain.vo.SocialInfoVO;
import com.smartLive.interaction.mapper.FollowMapper;
import com.smartLive.interaction.service.IFollowService;
import com.smartLive.interaction.strategy.identity.IdentityStrategy;
import com.smartLive.interaction.tool.QueryRedisSourceIdsTool;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartLive.user.api.domain.BlogDTO;
import com.smartLive.user.api.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;

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


    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RemoteAppUserService remoteAppUserService;
    /**
     * 策略模式
     */
    @Autowired
    private Map<String, IdentityStrategy> identityStrategyMap;

    @Autowired
    private QueryRedisSourceIdsTool queryRedisSourceIdsTool;
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
    public Result follow(Follow follow) {
        //获取当前用户id
        Long userId = UserContextHolder.getUser().getId();
        // 1. 获取对应的枚举策略
        IdentityTypeEnum followType = IdentityTypeEnum.getByCode(follow.getSourceType());
        if (followType == null) {
            return Result.fail("关注类型错误");
        }
        //2.我的关注列表
        String myFollowKey = followType.getFollowKeyPrefix() + userId;

        // 3. 对方的粉丝列表
        String targetFansKey = followType.getFansKeyPrefix() + follow.getSourceId();
        //判断是关注还是取关
        if(follow.getIsFollow()){
            //关注
            follow.setUserId(userId);
            boolean save = save(follow);
            if (save) {
                //关注成功，添加关注到redis
                stringRedisTemplate.opsForZSet().add(myFollowKey, follow.getSourceId().toString(), System.currentTimeMillis());
                stringRedisTemplate.opsForZSet().add(targetFansKey, userId.toString(), System.currentTimeMillis());
            }
        }else{
            //取关
            boolean remove = remove(new QueryWrapper<Follow>().eq("user_id", userId).eq("source_type",follow.getSourceType()).eq("source_id", follow.getSourceId()));
            if (remove) {
                //取关成功，从redis中删除关注
                stringRedisTemplate.opsForZSet().remove(myFollowKey, follow.getSourceId().toString());
                stringRedisTemplate.opsForZSet().remove(targetFansKey, userId.toString());
            }
        }
        return Result.ok();
    }

    /**
     * 判断是否关注
     *
     * @param follow
     * @return
     */
    @Override
    public Result isFollowed(Follow follow) {
        com.smartLive.common.core.domain.UserDTO user = UserContextHolder.getUser();
        if (user == null) {
            return Result.ok(false);
        }
        //获取当前用户id
        Long userId = user.getId();
        // 1. 获取对应的枚举策略
        IdentityTypeEnum followType = IdentityTypeEnum.getByCode(follow.getSourceType());
        if (followType == null) {
            return Result.fail("关注类型错误");
        }
        String key =followType.getFollowKeyPrefix()+userId;
//        //判断是否关注 从数据库中查询
//        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        //判断是否关注 从redis的zSet集合中查询
        //如果分数不为 null，说明元素存在（已关注）；如果为 null，说明不存在（未关注）
        Boolean isFollow = stringRedisTemplate.opsForZSet().score(key, follow.getSourceId().toString()) != null;
        return Result.ok(isFollow);
    }

    /**
     * 共同关注 userId
     *
     * @param
     * @return
     */
    @Override
    public Result common(Follow follow, Integer current) {
        IdentityTypeEnum identityTypeEnum = IdentityTypeEnum.getByCode(follow.getSourceType());
        if (identityTypeEnum == null) {
            return Result.fail("关注类型错误");
        }
        //获取当前用户id
        Long currentUserId = UserContextHolder.getUser().getId();
        Page<Long> commonFollowPage =  queryRedisSourceIdsTool.queryRedisCommonFollowIdPage(identityTypeEnum.getFollowKeyPrefix(), currentUserId, follow.getUserId(), current, SystemConstants.DEFAULT_PAGE_SIZE);
        if (commonFollowPage.getTotal()==0) {
            return Result.ok(null);
        }
        List<Long> idList = commonFollowPage.getRecords();
        IdentityStrategy identityStrategy = identityStrategyMap.get(identityTypeEnum.getBizDomain()+"IdentityStrategy");
        List<SocialInfoVO> socialInfoVOList = identityStrategy.getFollowList(idList);
          return Result.ok(socialInfoVOList);
    }

    /**
     * 发送博客给关注者
     *
     * @param blogDTO
     */
    @Override
    public void sendBlogToFollowers(BlogDTO blogDTO) {
        //从redis里面读取粉丝列表
        IdentityTypeEnum followType = IdentityTypeEnum.getByCode(GlobalBizTypeEnum.USER.getCode());
        String fansKey = followType.getFansKeyPrefix() + blogDTO.getUserId();
        //推送笔记id给所有粉丝
        // 查询笔记作者下的所有粉丝
        List<Follow> followList = query().eq("source_type", GlobalBizTypeEnum.USER.getCode()).eq("source_id", blogDTO.getUserId()).list();
        for (Follow follow : followList) {
            //获取粉丝id
            Long userId = follow.getUserId();
            //推送
            String key = RedisConstants.FEED_KEY + userId;
            stringRedisTemplate.opsForZSet().add(key, blogDTO.getId().toString(), System.currentTimeMillis());
        }
    }

    /**
     * 推送数据给粉丝
     *
     * @param feedEventDTO
     */
    @Override
    public void pushToFollowers(FeedEventDTO feedEventDTO) {
        //从redis里面读取粉丝列表
        IdentityTypeEnum followType = IdentityTypeEnum.getByCode(feedEventDTO.getSourceType());
        if (followType == null) {
            log.error("推送数据给粉丝失败，未知的关注类型");
            return;
        }
        String fansKey = followType.getFansKeyPrefix() + feedEventDTO.getSourceId();
        Set<String> userIdSet= stringRedisTemplate.opsForZSet().range(fansKey, 0, -1);
        List<Long> userIdList = userIdSet.stream().map(Long::valueOf).collect(Collectors.toList());
        //推送笔记id给所有粉丝
        // 查询笔记作者下的所有粉丝
        if(userIdList.isEmpty()){
            userIdList = query().select("user_id").eq("source_type", feedEventDTO.getSourceType()).eq("source_id", feedEventDTO.getSourceId()).list().stream().map(Follow::getUserId).collect(Collectors.toList());
        }
        if(userIdList.isEmpty()){
            log.info("推送数据给粉丝失败，没有粉丝");
            return;
        }
        for (Long userId : userIdList) {
            //推送
            String feedKeyPrefix = FeedTypeEnum.getByCode(feedEventDTO.getBizType()).getFeedKeyPrefix();
            String key = feedKeyPrefix + userId;
            stringRedisTemplate.opsForZSet().add(key, feedEventDTO.getBizId().toString(), System.currentTimeMillis());
        }
    }
    /**
     * 获取粉丝列表
     *
     * @return
     */
    @Override
    public Result getFans(Follow follow,Integer current) {
        // 1. 获取对应的枚举策略
        IdentityTypeEnum followType = IdentityTypeEnum.getByCode(follow.getSourceType());
        if (followType == null) {
            return Result.fail("关注类型错误");
        }
        //从redis获取
        Page<Long> fanIdPage = queryRedisSourceIdsTool.queryRedisIdPage(followType.getFansKeyPrefix(), follow.getSourceId(),current, SystemConstants.DEFAULT_PAGE_SIZE);
        List<Long> userIdList = fanIdPage.getRecords();
        //redis获取失败，从数据库获取
        if (userIdList.isEmpty()) {
            //获取粉丝id
            userIdList = query()
                    .select("user_id")
                    .eq("source_type",follow.getSourceType())
                    .eq("source_id", follow.getSourceId())
                    .orderByDesc("create_time") // 添加排序
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE))
                    .getRecords()                .stream()
                    .map(Follow::getUserId)  // 假设 follow 是你的实体类
                    .collect(Collectors.toList());
        }
        //根据id查询用户
       if(userIdList.isEmpty()){
           return Result.ok(Collections.emptyList());
       }
        R<List<User>> userSuccess = remoteAppUserService.getUserList(userIdList);
        if (userSuccess.getCode() != 200) {
            return Result.fail(userSuccess.getMsg());
        }
        List<User> userList = userSuccess.getData();
        userList.forEach(user->{
            user.setIsFollow((Boolean) isFollowed(new Follow(GlobalBizTypeEnum.USER.getCode(), user.getId())).getData());
        });
        return Result.ok(userList);
    }

    /**
     * 获取关注列表
     *
     * @param follow
     * @return
     */
    @Override
    public Result getFollows(Follow follow,Integer current) {
//        // 1. 获取对应的枚举策略
        IdentityTypeEnum identityType = IdentityTypeEnum.getByCode(follow.getSourceType());
        if (identityType == null) {
            return Result.fail("关注类型错误");
        }
        //根据关注类型从关注策略工程获取bean
        IdentityStrategy identityStrategy = identityStrategyMap.get(identityType.getBizDomain()+"IdentityStrategy");
        //从redis获取
        Page<Long> fanIdPage = queryRedisSourceIdsTool.queryRedisIdPage(identityType.getFollowKeyPrefix(), follow.getUserId(),current, SystemConstants.DEFAULT_PAGE_SIZE);
        List<Long> sourceIdList = fanIdPage.getRecords();
        //redis获取失败，从数据库获取
        if (sourceIdList.isEmpty()) {
            //获取粉丝id
            sourceIdList = query()
                    .select("user_id")
                    .eq("source_type",follow.getSourceType())
                    .eq("source_id", follow.getSourceId())
                    .orderByDesc("create_time") // 添加排序
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE))
                    .getRecords()                .stream()
                    .map(Follow::getUserId)  // 假设 follow 是你的实体类
                    .collect(Collectors.toList());
        }
                if(sourceIdList.isEmpty()){
                    return Result.ok(Collections.emptyList());
                }
        List<SocialInfoVO> socialInfoVOList = identityStrategy.getFollowList(sourceIdList);
        return Result.ok(socialInfoVOList);
    }

    /**
     * 获取关注数
     *
     * @param follow
     * @return
     */
    @Override
    public Integer getFollowCount(Follow follow) {
        int followCount = query().eq("source_type", follow.getSourceType()).eq("source_id",follow.getSourceId()).eq("user_id", follow.getUserId()).count().intValue();
        return followCount;
    }

    /**
     * 获取粉丝数
     *
      * @param follow
     * @return
     */
    @Override
    public Integer getFanCount(Follow follow) {
        int fansCount = query().eq("source_type", follow.getSourceType()).eq("source_id", follow.getSourceId()).count().intValue();
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
        Long currentUserId=UserContextHolder.getUser().getId();
        // 提取关注用户ID列表
        List<Long> targetUserFollowIds = lambdaQuery()
                .select(Follow::getSourceId)
                .eq(Follow::getSourceType, follow.getSourceType())
                .eq(Follow::getSourceId, follow.getSourceId())
                .eq(Follow::getUserId, follow.getUserId())
                .list()
                .stream()
                .map(Follow::getSourceId)
                .collect(Collectors.toList());
        int commonFollowCount;
        // 如果目标用户没有关注任何人，直接返回0
        if (targetUserFollowIds.isEmpty()) {
            commonFollowCount = 0;
        } else {
            // 查询共同关注数
            commonFollowCount = lambdaQuery()
                    .select(Follow::getSourceId)
                    .eq(Follow::getSourceType, follow.getSourceType())
                    .eq(Follow::getSourceId, follow.getSourceId())
                    .eq(Follow::getUserId, currentUserId)
                    .in(Follow::getSourceId, targetUserFollowIds)
                    .count()
                    .intValue();
        }
        return commonFollowCount;
    }
}
