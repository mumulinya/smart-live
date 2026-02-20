package com.smartLive.user.service.impl;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import com.smartLive.common.redis.util.RedisBatchCacheUtil;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
import com.smartLive.common.core.enums.GlobalBizTypeEnum;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.rabbitmq.utils.MqMessageSendUtils;
import com.smartLive.common.redis.service.RedisService;
import com.smartLive.common.security.utils.SecurityUtils;
import com.smartLive.interaction.api.RemoteCommentService;
import com.smartLive.interaction.api.RemoteFollowService;
import com.smartLive.interaction.api.RemoteLikeService;
import com.smartLive.interaction.api.RemoteStarService;
import com.smartLive.interaction.api.DTO.FollowDTO;
import com.smartLive.interaction.api.DTO.LikeDTO;
import com.smartLive.interaction.api.DTO.StarDTO;
import com.smartLive.order.api.RemoteOrderService;
import com.smartLive.user.api.domain.UserDTO;
import com.smartLive.user.domain.Stats;
import com.smartLive.user.domain.UserInfo;
import com.smartLive.user.domain.VO.UserInfoVO;
import com.smartLive.user.domain.VO.UserVO;
import com.smartLive.user.service.IUserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.smartLive.user.mapper.UserMapper;
import com.smartLive.user.domain.User;
import com.smartLive.user.service.IUserService;
import static com.smartLive.common.core.constant.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * 用户Service业务层处理
 *
 * @author mumulin
 * @date 2025-09-21
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService
{
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisService redisService;
    @Autowired
    private IUserInfoService userInfoService;
    @Autowired
    private RedisBatchCacheUtil redisBatchCacheUtil;

    @Autowired
    private RemoteBlogService remoteBlogService;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    private RemoteFollowService remoteFollowService;
    @Autowired
    private RemoteLikeService remoteLikeService;
    @Autowired
    private RemoteStarService remoteStarService;

    /**
     * 将User实体转换为UserVO
     * @param user User实体
     * @return UserVO对象
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
     * 将User列表转换为UserVO列表
     * @param userList User实体列表
     * @return UserVO列表
     */
    private List<UserVO> convertToUserVOList(List<User> userList) {
        if (userList == null || userList.isEmpty()) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::convertToUserVO)
                .collect(Collectors.toList());
    }
    /**
     * 查询用户
     *
     * @param id 用户主键
     * @return 用户
     */
    @Override
    public User selectUserById(Long id)
    {
        return userMapper.selectUserById(id);
    }

    /**
     * 查询用户列表
     *
     * @param user 用户
     * @return 用户
     */
    @Override
    public List<User> selectUserList(User user)
    {
        return userMapper.selectUserList(user);
    }

    /**
     * 新增用户
     *
     * @param user 用户
     * @return 结果
     */
    @Override
    public int insertUser(User user)
    {
        user.setCreateTime(DateUtils.getNowDate());
        return userMapper.insertUser(user);
    }

    /**
     * 修改用户
     *
     * @param user 用户
     * @return 结果
     */
    @Override
    public int updateUser(User user)
    {
        user.setUpdateTime(DateUtils.getNowDate());
        int i = userMapper.updateUser(user);
        if(i>0){
            com.smartLive.common.core.domain.UserDTO dto = UserContextHolder.getUser();
            //更新用户缓存信息
            if(dto!=null){
                String tokenKey = dto.getToken();
                User userById = getById(user.getId());
                UserDTO userDTO= BeanUtil.copyProperties(userById, UserDTO.class);
                //存储
                Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                        CopyOptions.create()
                                //忽略空值
                                .setIgnoreNullValue(true)
                                //把userDto字段值转为字符串
                                .setFieldValueEditor((fieldName, fieldValue) -> fieldValue == null ? "" : fieldValue.toString()));
                //更新之前的数据
                redisService.setCacheMap(tokenKey,userMap);
                //设置token有效期
                redisService.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
                UserContextHolder.removeUser();
            }
            //更新es数据
            publish(new String[]{user.getId().toString()});
            sendAuditMessage(user);
        }
        return i;
    }
    /**
     * 发送审核消息
     * @param user
     */
    private void sendAuditMessage(User user) {
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
        MqMessageSendUtils.sendMqMessage(rabbitTemplate, MqConstants.AUDIT_EXCHANGE_NAME,MqConstants.AUDIT_ROUTING_KEY, auditMessage);
    }
    /**
     * 批量删除用户
     *
     * @param ids 需要删除的用户主键
     * @return 结果
     */
    @Override
    public int deleteUserByIds(Long[] ids)
    {
//        int i = userMapper.deleteShopByIds(ids);
//        //删除es数据
//        if (i > 0) {
        for (Long id : ids) {
            executorService.submit(()->{
                log.info("线程：{}开始删除es数据id：{}",Thread.currentThread().getName(),id);
                ContentSyncMessage contentSyncMessage = new ContentSyncMessage();
                contentSyncMessage.setId(id);
                contentSyncMessage.setIndexName(EsIndexNameConstants.USER_INDEX_NAME);
                    contentSyncMessage.setType(GlobalBizTypeEnum.USER.getCode());
                //发起rabbitMq信息删除
//                rabbitTemplate.convertAndSend(MqConstants.ES_EXCHANGE,MqConstants.ES_ROUTING_USER_DELETE,esInsertRequest);
                MqMessageSendUtils.sendMqMessage(rabbitTemplate, MqConstants.ES_EXCHANGE, MqConstants.ES_ROUTING_DELETE, contentSyncMessage);
            });
        }
//        }
        return 1;
    }

    /**
     * 删除用户信息
     *
     * @param id 用户主键
     * @return 结果
     */
    @Override
    public int deleteUserById(Long id)
    {
        return userMapper.deleteUserById(id);
    }

    /**
     * 根据用户电话号码查询用户
     *
     * @param phone 手机号
     * @return 用户
     */
    @Override
    public User getUserInfoByPhone(String phone) {
        User user = query().eq("phone", phone).one();
        return user;
    }

    /**
     * 电话号码创建用户
     *
     * @param phone 手机号
     * @return 用户
     */
    @Override
    public User createUserByPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }

    /**
     * 根据用户id列表查询用户列表
     *
     * @param userIdList 用户id列表
     * @return 用户列表
     */
    @Override
    public List<UserVO> getUserList(List<Long> userIdList) {
        // 1. Utilize RedisBatchCacheUtil for cached batch retrieval (UserVO with static info)
        List<UserVO> userVOList = redisBatchCacheUtil.queryBatchWithCache(
                RedisConstants.CACHE_USER_KEY,
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
            for (UserVO userVO : userVOList) {
               if (userVO != null) {
                   isFollow(userVO);
               }
            }
        }
        return userVOList;
    }

    /**
     * 根据用户id查询用户
     *
     * @param id 用户id
     * @return 用户
     */
    @Override
    public UserVO queryUserById(Long id) {
        User user = getById(id);
        if(user!= null){
            //查询用户是否关注当前用户
            isFollow(user);
        }
        return convertToUserVO(user);
    }

    /**
     * 查询用户info信息
     *
     * @param user 用户
     */
    private void  queryUserInfo(User user){
        UserInfoVO userInfo = userInfoService.getByUserId(user.getId());
        if (userInfo != null){
            user.setIntroduce(userInfo.getIntroduce());
            user.setCity(userInfo.getCity());
        }
    }

    /**
     * 修改用户密码
     *
     * @param user
     * @return
     */
    @Override
    public Boolean updateUserPassWord(User user) {
        User byId = getById(user.getId());
        if (byId != null){
            if(byId.getPassword() == null){
                throw new BusinessException("用户密码不能为空");
            }
            String rawPassword = byId.getPassword();
            if(user.getNewPassword() == null){
                throw new BusinessException("新密码不能为空");
            }
            if(user.getOldPassword()== null){
                throw new BusinessException("旧密码不能为空");
            }
            String oldPassword = user.getOldPassword();
            if(SecurityUtils.matchesPassword(rawPassword,oldPassword)){
                throw new BusinessException("旧密码输入错误");
            }
            byId.setPassword(SecurityUtils.encryptPassword(user.getNewPassword()));
            return updateById(byId);
        }
        return false;
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
        CountDownLatch countDownLatch = new CountDownLatch(7);
        //获取粉丝数
        Future<Integer> fanCountFuture = executorService.submit(() -> {
            log.info("线程：{}开始查询粉丝数",Thread.currentThread().getName());
            FollowDTO followDTO=new FollowDTO();
            followDTO.setSourceType(GlobalBizTypeEnum.USER.getCode());
            followDTO.setSourceId(userId);
            Integer fanCount = remoteFollowService.getFanCount(followDTO);
            countDownLatch.countDown();
            return fanCount;
        });
        //获取关注数
        Future<Integer> followCountFuture = executorService.submit(() -> {
            log.info("线程：{}开始查询关注数",Thread.currentThread().getName());
            FollowDTO followDTO=new FollowDTO();
            followDTO.setSourceType(GlobalBizTypeEnum.USER.getCode());
            followDTO.setUserId(userId);
            Integer followCount = remoteFollowService.getFollowCount(followDTO);
            countDownLatch.countDown();
            return followCount;
        });
        // 当前用户
        com.smartLive.common.core.domain.UserDTO user = UserContextHolder.getUser();
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
//        //获取发表评论数量
//        Future<Integer> commentCountFuture = executorService.submit(() -> {
//            log.info("线程：{}开始查询发表评论数",Thread.currentThread().getName());
//            CommentDTO commentDTO = new CommentDTO();
//            commentDTO.setUserId(userId);
//            Integer commentCount = remoteCommentService.getCommentCount(commentDTO);
//            countDownLatch.countDown();
//            return commentCount;
//        });
//        //获取订单数量
//        Future<Integer> orderCountFuture = executorService.submit(() -> {
//            log.info("线程：{}开始查询订单数",Thread.currentThread().getName());
//            Integer orderCount =  remoteOrderService.getOrderCount(userId);
//            countDownLatch.countDown();
//            return orderCount;
//        });
//        //获取关注店铺数量
//        Future<Integer> followShopCountFuture = executorService.submit(() -> {
//            log.info("线程：{}开始查询收藏数",Thread.currentThread().getName());
//            FollowDTO followDTO=new FollowDTO();
//            followDTO.setSourceType(GlobalBizTypeEnum.SHOP.getCode());
//            followDTO.setUserId(userId);
//            Integer collectCount = remoteFollowService.getFollowCount(followDTO);
//            countDownLatch.countDown();
//            return collectCount;
//        });
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
            Stats stats= Stats.builder()
                    .blogCount(blogCountFuture.get())
                    .followCount(66)
                    .commonFollowCount(commonFollowCountFuture.get())
                    .fansCount(10000000)
                    .likeCount(likeCountFuture.get())
//                    .commentCount(commentCountFuture.get())
//                    .orderCount(orderCountFuture.get())
//                    .followShopCount(followShopCountFuture.get())
                    .blogLikeCount(blogLikeCountFuture.get())
                    .blogStarCount(blogStarCountFuture.get())
                    .build();
            return stats;
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
        User user = query().eq("id", id).one();
        UserInfoVO userInfo = userInfoService.getByUserId(user.getId());
        UserVO userVO = convertToUserVO(user);
        userVO.setIntroduce(userInfo.getIntroduce());
        userVO.setBackgroundImage(userInfo.getBackgroundImage());
        userVO.setCity(userInfo.getCity());
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
                users.forEach(
                        user -> {
                            UserInfoVO userInfo = userInfoMap.get(user.getId());
                            if(userInfo != null){
                                user.setIntroduce(userInfo.getIntroduce());
                                user.setCity(userInfo.getCity());
                            }
                        }
                );
                // 发送批量消息
                sendUserBatchMessage(users);
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
                users.forEach(user -> {
                    UserInfoVO userInfo = userInfoMap.get(user.getId());
                    if(userInfo != null){
                        user.setIntroduce(userInfo.getIntroduce());
                        user.setCity(userInfo.getCity());
                    }
                });
                
                // Batch send message
                sendUserBatchMessage(users);
            }
        });
        return "发布成功";
    }

    /**
     * 批量发送ES同步消息
     * @param users
     */
    private void sendUserBatchMessage(List<User> users) {
        if (CollUtil.isEmpty(users)) {
            return;
        }
        ContentBatchSyncMessage request = new ContentBatchSyncMessage();
        request.setIndexName(EsIndexNameConstants.USER_INDEX_NAME);
        request.setData(users);
        request.setType(GlobalBizTypeEnum.USER.getCode());
        
        // 发送rabbitmq消息数据插入es
        MqMessageSendUtils.sendMqMessage(rabbitTemplate, MqConstants.ES_EXCHANGE, MqConstants.ES_ROUTING_BATCH_INSERT, request);
    }

    /**
     * 判断用户是否被当前用户关注
     * @param user
     */
    private void isFollow(User user){
        FollowDTO followDTO=new FollowDTO();
        followDTO.setSourceType(GlobalBizTypeEnum.USER.getCode());
        followDTO.setSourceId(user.getId());
        Boolean isFollow = remoteFollowService.isFollowed(followDTO);
        user.setIsFollow(isFollow);
    }

    /**
     * 判断用户是否被当前用户关注 (UserVO version)
     * @param userVO
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

}