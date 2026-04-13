package com.smartLive.user.service.impl;
import com.smartLive.common.core.constant.mq.SearchMqConstants;
import com.smartLive.common.core.constant.mq.AiAuditMqConstants;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.smartLive.common.core.domain.AppLoginUser;
import com.smartLive.common.redis.util.CacheClient;
import com.smartLive.common.redis.util.RedisMultiCacheManager;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.common.core.constant.*;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.exception.BusinessException;
import com.smartLive.common.core.utils.bean.BeanUtils;
import com.smartLive.common.rabbitmq.domain.AuditMessage;
import com.smartLive.common.rabbitmq.domain.ContentBatchSyncMessage;
import com.smartLive.common.rabbitmq.domain.ContentSyncMessage;
import com.smartLive.common.rabbitmq.domain.MqSendMode;
import com.smartLive.common.core.enums.common.AuditStatusEnum;
import com.smartLive.common.core.enums.common.GlobalBizTypeEnum;
import com.smartLive.common.core.enums.interaction.FollowTypeEnum;
import com.smartLive.common.core.enums.interaction.LikeTypeEnum;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.common.security.utils.SecurityUtils;
import com.smartLive.interaction.api.RemoteFollowService;
import com.smartLive.interaction.api.RemoteLikeService;
import com.smartLive.interaction.api.RemoteStarService;
import com.smartLive.interaction.api.DTO.FollowDTO;
import com.smartLive.interaction.api.DTO.LikeDTO;
import com.smartLive.interaction.api.DTO.StarDTO;
import com.smartLive.user.api.domain.UserDTO;
import com.smartLive.user.domain.Stats;
import com.smartLive.user.domain.UserInfo;
import com.smartLive.user.domain.VO.UserInfoVO;
import com.smartLive.user.domain.VO.UserVO;
import com.smartLive.user.service.IUserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smartLive.user.mapper.UserMapper;
import com.smartLive.user.domain.User;
import com.smartLive.user.service.IUserService;
import static com.smartLive.common.core.constant.SystemConstants.USER_NICK_NAME_PREFIX;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService
{
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisService redisService;
    @Autowired
    private CacheClient cacheClient;
    @Autowired
    private IUserInfoService userInfoService;
    @Autowired
    private RedisMultiCacheManager redisMultiCacheManager;

    @Autowired
    private RemoteBlogService remoteBlogService;

    
    @Autowired
    private MqMessageSendUtils mqMessageSendUtils;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    private RemoteFollowService remoteFollowService;
    @Autowired
    private RemoteLikeService remoteLikeService;
    @Autowired
    private RemoteStarService remoteStarService;
    /**
     * 将User实体转换为UserVO对象
     */

    private UserVO convertToUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }
    /**
     * 批量将User实体转换为UserVO对象
     */

    private List<UserVO> convertToUserVOList(List<User> userList) {
        if (userList == null || userList.isEmpty()) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::convertToUserVO)
                .collect(Collectors.toList());
    }

    private void fillUserDynamicStats(UserVO userVO) {
        fillUserDynamicStats(Collections.singletonList(userVO));
    }

    private void fillUserDynamicStats(List<UserVO> userVOList) {
        if (CollUtil.isEmpty(userVOList)) {
            return;
        }
        List<UserVO> validUserVOList = userVOList.stream()
                .filter(userVO -> userVO != null && userVO.getId() != null)
                .toList();
        if (CollUtil.isEmpty(validUserVOList)) {
            return;
        }
        List<Long> userIds = validUserVOList.stream()
                .map(UserVO::getId)
                .distinct()
                .collect(Collectors.toList());

        String followKeyPrefix = FollowTypeEnum.USER_IDENTITY.getFollowCountKeyPrefix();
        String fansKeyPrefix = FollowTypeEnum.USER_IDENTITY.getFansCountKeyPrefix();
        String likedKeyPrefix = LikeTypeEnum.USER_LIKE.getLikedCountKeyPrefix();

        List<Integer> followValues = redisService.getMultiCacheObject(userIds.stream().map(id -> followKeyPrefix + id).collect(Collectors.toList()));
        List<Integer> fansValues = redisService.getMultiCacheObject(userIds.stream().map(id -> fansKeyPrefix + id).collect(Collectors.toList()));
        List<Integer> likedValues = redisService.getMultiCacheObject(userIds.stream().map(id -> likedKeyPrefix + id).collect(Collectors.toList()));

        Map<Long, Integer> followMap = new HashMap<>(userIds.size());
        Map<Long, Integer> fansMap = new HashMap<>(userIds.size());
        Map<Long, Integer> likedMap = new HashMap<>(userIds.size());
        Set<Long> missingIds = new HashSet<>();
        fillCounterMapFromRedis(userIds, followValues, followMap, missingIds);
        fillCounterMapFromRedis(userIds, fansValues, fansMap, missingIds);
        fillCounterMapFromRedis(userIds, likedValues, likedMap, missingIds);

        if (CollUtil.isNotEmpty(missingIds)) {
            Map<Long, UserInfoVO> fallbackUserInfoMap = userInfoService.listByUserIds(new ArrayList<>(missingIds))
                    .stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(UserInfoVO::getUserId, java.util.function.Function.identity(), (left, right) -> left));
            Map<String, Integer> cacheMap = new HashMap<>(missingIds.size() * 3);
            for (Long userId : missingIds) {
                UserInfoVO userInfo = fallbackUserInfoMap.get(userId);
                if (!followMap.containsKey(userId)) {
                    Integer followee = userInfo != null && userInfo.getFollowee() != null ? userInfo.getFollowee() : 0;
                    followMap.put(userId, followee);
                    cacheMap.put(followKeyPrefix + userId, followee);
                }
                if (!fansMap.containsKey(userId)) {
                    Integer fans = userInfo != null && userInfo.getFans() != null ? userInfo.getFans() : 0;
                    fansMap.put(userId, fans);
                    cacheMap.put(fansKeyPrefix + userId, fans);
                }
                if (!likedMap.containsKey(userId)) {
                    Integer liked = userInfo != null && userInfo.getLiked() != null ? userInfo.getLiked() : 0;
                    likedMap.put(userId, liked);
                    cacheMap.put(likedKeyPrefix + userId, liked);
                }
            }
            if (!cacheMap.isEmpty()) {
                redisService.setMultiCacheObject(cacheMap);
            }
        }

        validUserVOList.forEach(userVO -> {
            Long userId = userVO.getId();
            userVO.setFollowee(followMap.getOrDefault(userId, 0));
            userVO.setFans(fansMap.getOrDefault(userId, 0));
            userVO.setLiked(likedMap.getOrDefault(userId, 0));
        });
    }

    private void fillCounterMapFromRedis(List<Long> ids, List<Integer> values, Map<Long, Integer> counterMap, Set<Long> missingIds) {
        for (int i = 0; i < ids.size(); i++) {
            Integer value = values != null && values.size() > i ? values.get(i) : null;
            if (value != null) {
                counterMap.put(ids.get(i), value);
            } else {
                missingIds.add(ids.get(i));
            }
        }
    }
    /**
     * 根据ID查询用户信息
     */
    @Override
    public User selectUserById(Long id)
    {
        return userMapper.selectUserById(id);
    }
    /**
     * 查询用户列表
     */

    @Override
    public List<User> selectUserList(User user)
    {
        return userMapper.selectUserList(user);
    }
    /**
     * 新增用户
     */

    @Override
    public int insertUser(User user)
    {
        user.setCreateTime(DateUtils.getNowDate());
        return userMapper.insertUser(user);
    }
    /**
     * 修改用户
     */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateUser(User user)
    {
        user.setUpdateTime(DateUtils.getNowDate());
        int i = userMapper.updateUser(user);
        if(i>0){
            User latestUser = getById(user.getId());
            if (latestUser == null) {
                throw new BusinessException("用户不存在");
            }
            userInfoService.update(new UpdateWrapper<UserInfo>()
                    .eq("user_id", user.getId())
                    .set("audit_status", AuditStatusEnum.WAITING.getCode()));
            sendAuditMessage(latestUser, MqSendMode.SYNC_RETRY_THROW);
            AppLoginUser dto = UserContextHolder.getUser();
            if(dto!=null){
                String tokenKey = dto.getToken();
                UserDTO userDTO= BeanUtil.copyProperties(latestUser, UserDTO.class);
                Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                        CopyOptions.create()
                                .setIgnoreNullValue(true)
                                .setFieldValueEditor((fieldName, fieldValue) -> fieldValue == null ? "" : fieldValue.toString()));
                redisService.setCacheMap(tokenKey,userMap);
                redisService.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
                UserContextHolder.removeUser();
            }
            clearUserCache(user.getId());
            publish(new String[]{user.getId().toString()});
        }
        return i;
    }
    /**
     * 发送用户审核MQ消息
     */
    private void sendAuditMessage(User user) {
        sendAuditMessage(user, MqSendMode.ASYNC_RETRY);
    }

    private void sendAuditMessage(User user, MqSendMode sendMode) {
        UserInfoVO userInfo = userInfoService.getByUserId(user.getId());
        UserVO userVO = convertToUserVO(user);
        userVO.setIntroduce(userInfo.getIntroduce());
        userVO.setBackgroundImage(userInfo.getBackgroundImage());
        userVO.setCity(userInfo.getCity());

        AuditMessage auditMessage = AuditMessage.builder()
                .bizId(user.getId())
                .bizType(GlobalBizTypeEnum.USER.getCode())
                .submitterId(user.getId())
                .auditContent(BeanUtil.beanToMap(userVO))
                .createTime(user.getCreateTime())
                .build();
        mqMessageSendUtils.sendMqMessage(
                AiAuditMqConstants.AUDIT_DIRECT_EXCHANGE,
                AiAuditMqConstants.AUDIT_ROUTING_KEY,
                auditMessage,
                sendMode
        );
    }
    /**
     * 批量删除用户
     */
    @Override
    public int deleteUserByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        int i = userMapper.deleteUserByIds(ids);
        if (i > 0) {
            clearUserCacheBatch(Arrays.asList(ids));
            for (Long id : ids) {
                executorService.submit(()->{
                    log.info("Deleting user {} from search indexes on thread {}", id, Thread.currentThread().getName());
                    ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
                    contentSyncMessage.setId(id);
                    contentSyncMessage.setIndexName(EsIndexNameConstants.USER_INDEX_NAME);
                    contentSyncMessage.setType(GlobalBizTypeEnum.USER.getCode());
                    mqMessageSendUtils.sendMqMessage( SearchMqConstants.ES_SYNC_EXCHANGE, SearchMqConstants.ES_SYNC_DELETE_ROUTING_KEY, contentSyncMessage);
                });
            }
        }
        return i;
    }
    /**
     * 删除单个用户
     */

    @Override
    public int deleteUserById(Long id)
    {
        int i = userMapper.deleteUserById(id);
        if (i > 0) {
            clearUserCache(id);
        }
        return i;
    }
    /**
     * 根据手机号查询用户信息
     */

    @Override
    public User getUserInfoByPhone(String phone) {
        User user = query().eq("phone", phone).one();
        return user;
    }
    /**
     * 根据手机号创建用户
     */

    @Override
    public User createUserByPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(user.getId());
        userInfo.setFans(0);
        userInfo.setFollowee(0);
        userInfo.setLiked(0);
        userInfo.setCreateTime(DateUtils.getNowDate());
        userInfoService.save(userInfo);

        return user;
    }
    /**
     * 根据ID列表批量查询用户信息
     */

    @Override
    public List<UserVO> getUserList(List<Long> userIdList) {
        // 1. Utilize RedisBatchCacheUtil for cached batch retrieval (UserVO with static info)
        List<UserVO> userVOList = redisMultiCacheManager.queryBatchWithCache(
                RedisConstants.CACHE_USER_KEY,
                RedisConstants.LOCK_USER_KEY,
                userIdList,
                UserVO.class,
                missingIds -> {
                    // DB Fallback
                    String idStr = StrUtil.join(",", missingIds);
                    List<User> users = query().in("id", missingIds)
                            .last("order by field(id," + idStr + ")")
                            .list();

                    return users.stream().map(user -> {
                        UserVO userVO = convertToUserVO(user);
                        if (userVO != null) {
                            UserInfoVO userInfo = userInfoService.getByUserId(user.getId());
                            if (userInfo != null) {
                                userVO.setIntroduce(userInfo.getIntroduce());
                                userVO.setCity(userInfo.getCity());
                                userVO.setBackgroundImage(userInfo.getBackgroundImage());
                            }
                        }
                        return userVO;
                    }).collect(Collectors.toList());
                },
                UserVO::getId,
                RedisConstants.CACHE_USER_TTL,
                TimeUnit.MINUTES
        );

        // 2. Populate dynamic info (isFollow) which depends on current user context
        if (CollUtil.isNotEmpty(userVOList)) {
            fillUserDynamicStats(userVOList);
            for (UserVO userVO : userVOList) {
               if (userVO != null) {
                   isFollow(userVO);
               }
            }
        }
        return userVOList;
    }
    /**
     * 根据ID获取用户信息 (整合缓存)
     */

    @Override
    public UserVO queryUserById(Long id) {
        UserVO userVO = cacheClient.queryWithLogicalExpireAndPassThrough(
                RedisConstants.CACHE_USER_KEY,
                RedisConstants.LOCK_USER_KEY,
                id,
                UserVO.class,
                this::loadUserDetailForCache,
                RedisConstants.CACHE_USER_TTL,
                TimeUnit.MINUTES
        );
        if(userVO != null){
            fillUserDynamicStats(userVO);
            isFollow(userVO);
        }
        return userVO;
    }
    /**
     * 修改用户密码
     */

    @Override
    public Boolean updateUserPassWord(Long userId, com.smartLive.user.DTO.PasswordDTO passwordDTO) {
        User byId = getById(userId);
        if (byId == null) {
            return false;
        }
        if (byId.getPassword() == null) {
            throw new BusinessException("password not set");
        }
        if (passwordDTO.getNewPassword() == null) {
            throw new BusinessException("new password is required");
        }
        if (passwordDTO.getOldPassword() == null) {
            throw new BusinessException("old password is required");
        }
        String rawPassword = byId.getPassword();
        String oldPassword = passwordDTO.getOldPassword();
        if (!SecurityUtils.matchesPassword(oldPassword, rawPassword)) {
            throw new BusinessException("old password is incorrect");
        }
        byId.setPassword(SecurityUtils.encryptPassword(passwordDTO.getNewPassword()));
        boolean updated = updateById(byId);
        if (updated) {
            clearUserCache(byId.getId());
        }
        return updated;
    }
    /**
     * 判断当前用户是否已关注该用户
     */

    private void isFollow(UserVO userVO){
        if (userVO == null) {
            return;
        }
        FollowDTO followDTO=new FollowDTO();
        followDTO.setSourceType(GlobalBizTypeEnum.USER.getCode());
        followDTO.setSourceId(userVO.getId());
        Boolean isFollow = remoteFollowService.isFollowed(followDTO);
        userVO.setIsFollow(isFollow);
    }
    /**
     * 为缓存加载用户详情数据
     */

    private UserVO loadUserDetailForCache(Long id) {
        User user = getById(id);
        if (user == null) {
            return null;
        }
        UserVO userVO = convertToUserVO(user);
        UserInfoVO userInfo = userInfoService.getByUserId(id);
        if (userInfo != null) {
            userVO.setIntroduce(userInfo.getIntroduce());
            userVO.setBackgroundImage(userInfo.getBackgroundImage());
            userVO.setCity(userInfo.getCity());
        }
        return userVO;
    }
    /**
     * 清除用户缓存
     */

    @Override
    public void clearUserCache(Long userId) {
        if (userId == null) {
            return;
        }
        redisService.deleteObject(RedisConstants.CACHE_USER_KEY + userId);
    }
    /**
     * 批量清除用户缓存
     */

    private void clearUserCacheBatch(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        userIds.stream().filter(Objects::nonNull).forEach(this::clearUserCache);
    }
    /**
     * 根据ID获取用户信息 (直接查询数据库)
     */

    @Override
    public UserVO getUserById(Long userId) {
        User userById = selectUserById(userId);
        if (userById != null) {
            UserVO userVO = convertToUserVO(userById);
            fillUserDynamicStats(userVO);
            return userVO;
        }
        return null;
    }
    /**
     * 获取用户统计信息
     *
     * @param userId 用户id
     * @return 用户统计信息
     */
    @Override
    public Stats getStats(Long userId) {
        //使用线程池＋future来实现
        CountDownLatch countDownLatch = new CountDownLatch(5);
        // 当前用户
        AppLoginUser user = UserContextHolder.getUser();
        // 获取共同关注数
        Future<Integer> commonFollowCountFuture = executorService.submit(() -> {
            Integer commonFollowCount = 0;
            // 判断当前查询用户是否是当前登录用户
            if (user != null&&user.getId() != userId) {
                Long currentUserId = user.getId();
                FollowDTO followDTO=new FollowDTO();
                followDTO.setSourceType(GlobalBizTypeEnum.USER.getCode());
                followDTO.setUserId(currentUserId);
                followDTO.setSourceId(userId);
                //获取共同关注数
                commonFollowCount =remoteFollowService.getCommonFollowCount(followDTO);
            }
            countDownLatch.countDown();
            return commonFollowCount;
        });
        //获取博客数
        Future<Integer> blogCountFuture = executorService.submit(() -> {
            log.info("线程：{}开始查询博客数",Thread.currentThread().getName());
            Integer blogCount =  remoteBlogService.getBlogCount(userId);
            countDownLatch.countDown();
            return blogCount;
        });
        //获取获赞数
        Future<Integer> likeCountFuture = executorService.submit(() -> {
            log.info("线程：{}开始查询点赞数",Thread.currentThread().getName());
            Integer likeCount = remoteBlogService.getLikeCount(userId);
            countDownLatch.countDown();
            return likeCount;
        });
        //获取用户点赞博客数
        Future<Integer> blogLikeCountFuture = executorService.submit(() -> {
            LikeDTO likeDTO=new LikeDTO();
            likeDTO.setSourceType(GlobalBizTypeEnum.BLOG.getCode());
            likeDTO.setUserId(userId);
            Integer blogLikeCount = remoteLikeService.getUserLikeCount(likeDTO);
            countDownLatch.countDown();
            return blogLikeCount;
        });
        //获取用户收藏博客数
        Future<Integer> blogStarCountFuture = executorService.submit(() -> {
            StarDTO starDTO=new StarDTO();
            starDTO.setSourceType(GlobalBizTypeEnum.BLOG.getCode());
            starDTO.setUserId(userId);
            Integer collectCount = remoteStarService.getUserStarCount(starDTO);
            countDownLatch.countDown();
            return collectCount;
        });
        try {
            log.info("开始获取用户统计信息");
            countDownLatch.await();
            log.info("获取用户统计信息结束");
            return Stats.builder()
                    .blogCount(blogCountFuture.get())
                    .commonFollowCount(commonFollowCountFuture.get())
                    .likeCount(likeCountFuture.get())
                    .blogLikeCount(blogLikeCountFuture.get())
                    .blogStarCount(blogStarCountFuture.get())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据用户id获取用户名称
     *
     * @param userId
     * @return
     */
    @Override
    public String getUserNameById(Long userId) {
        return query().select("nick_name")
                .eq("id", userId)
                .one()
                .getNickName();
    }

    /**
     * 根据用户id查询用户信息
     *
     * @param id
     * @return
     */
    @Override
    public UserVO queryUserInfoById(Long id) {
        UserVO userVO = cacheClient.queryWithLogicalExpireAndPassThrough(
                RedisConstants.CACHE_USER_KEY,
                RedisConstants.LOCK_USER_KEY,
                id,
                UserVO.class,
                this::loadUserDetailForCache,
                RedisConstants.CACHE_USER_TTL,
                TimeUnit.MINUTES
        );
        fillUserDynamicStats(userVO);
        return userVO;
    }

    /**
     * 全部发布
     *
     * @return 全部发布结果
     */
    @Override
    public String allPublish() {
        int page = PageConstants.PAGE_NUMBER;
        int pageSize = PageConstants.ES_PAGE_SIZE; // 每页50条
        while (true) {
            // 分页查询
            List<User> users = query()
                    .page(new Page<>(page, pageSize))
                    .getRecords();
            if (users.isEmpty()) {
                break;
            }
            int finalPage = page;

            executorService.submit(()->{
                log.info("线程：{}开始处理第 {} 页数据",Thread.currentThread().getName(), finalPage);
                List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
                List<UserInfoVO> userInfos = userInfoService.listByUserIds(userIds);
                Map<Long,UserInfoVO> userInfoMap= userInfos.stream().collect(Collectors.toMap(UserInfoVO::getUserId, userInfo -> userInfo));
                List<UserVO> userVOList = convertToUserVOList(users);
                userVOList.forEach(
                        userVO -> {
                            UserInfoVO userInfo = userInfoMap.get(userVO.getId());
                            if(userInfo != null){
                                userVO.setIntroduce(userInfo.getIntroduce());
                                userVO.setCity(userInfo.getCity());
                            }
                        }
                );
                // 发送批量消息
                sendUserBatchMessage(userVOList);
                log.info("发送第 {} 页，{} 条数据", finalPage, users.size());
            });
            page++;
        }
        return "数据发布完成";
    }


    /**
     * 发布
     *
     * @param
     * @return 发布结果
     */
    @Override
    public String publish(String[] ids) {
        if (ids == null || ids.length == 0) {
            return "参数为空";
        }
        // Convert to Long list
        List<Long> idList = Arrays.stream(ids)
                .map(Long::valueOf)
                .collect(Collectors.toList());

        executorService.submit(() -> {
            log.info("线程{}，开始批量发布用户：{}", Thread.currentThread().getName(), idList);
            // Batch query
            List<User> users = query().in("id", idList).list();
            if (CollUtil.isNotEmpty(users)) {
                // Batch populate user info
                List<UserInfoVO> userInfos = userInfoService.listByUserIds(idList);
                Map<Long,UserInfoVO> userInfoMap= userInfos.stream().collect(Collectors.toMap(UserInfoVO::getUserId, userInfo -> userInfo));
                List<UserVO> userVOList = convertToUserVOList(users);
                userVOList.forEach(userVO -> {
                    UserInfoVO userInfo = userInfoMap.get(userVO.getId());
                    if(userInfo != null){
                        userVO.setIntroduce(userInfo.getIntroduce());
                        userVO.setCity(userInfo.getCity());
                    }
                });

                // Batch send message
                sendUserBatchMessage(userVOList);
            }
        });
        return "发布成功";
    }

    /**
     * 批量发送ES同步消息
     * @param users
     */
    private void sendUserBatchMessage(List<?> users) {
        if (CollUtil.isEmpty(users)) {
            return;
        }
        ContentBatchSyncMessage request = new ContentBatchSyncMessage();
        request.setIndexName(EsIndexNameConstants.USER_INDEX_NAME);
        request.setData(users);
        request.setType(GlobalBizTypeEnum.USER.getCode());

        // 发送rabbitmq消息数据插入es
        mqMessageSendUtils.sendMqMessage( SearchMqConstants.ES_SYNC_EXCHANGE, SearchMqConstants.ES_SYNC_BATCH_INSERT_ROUTING_KEY, request);
    }

    /**
     * 搜索用户
     */

    @Override
    public List<UserVO> searchUsers(String keyword) {
        String trimmedKeyword = keyword == null ? null : keyword.trim();
        List<User> users = query()
                .select("id", "nick_name")
                .like(StrUtil.isNotBlank(trimmedKeyword), "nick_name", trimmedKeyword)
                .list();
        return convertToUserVOList(users);
    }
}
