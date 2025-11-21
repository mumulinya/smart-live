package com.smartLive.user.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.comment.api.RemoteCommentService;
import com.smartLive.common.core.constant.*;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.EsBatchInsertRequest;
import com.smartLive.common.core.domain.EsInsertRequest;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.follow.api.RemoteFollowService;
import com.smartLive.order.api.RemoteOrderService;

import com.smartLive.user.api.domain.UserDTO;
import com.smartLive.user.domain.Stats;
import com.smartLive.user.domain.UserInfo;
import com.smartLive.user.service.IUserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private IUserInfoService userInfoService;

    @Autowired
    private RemoteBlogService remoteBlogService;

    @Autowired
    private RemoteCommentService remoteCommentService;

    @Autowired
    private RemoteOrderService remoteOrderService;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    private RemoteFollowService remoteFollowService;


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
                stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
                //设置token有效期
                stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
                UserContextHolder.removeUser();
            }
            //更新es数据
            publish(new String[]{user.getId().toString()});
        }
        return i;
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
                EsInsertRequest esInsertRequest = new EsInsertRequest();
                esInsertRequest.setId(id);
                esInsertRequest.setIndexName(EsIndexNameConstants.USER_INDEX_NAME);
                esInsertRequest.setDataType(EsDataTypeConstants.USER);
                //发起rabbitMq信息删除
                rabbitTemplate.convertAndSend(MqConstants.ES_EXCHANGE,MqConstants.ES_ROUTING_USER_DELETE,esInsertRequest);
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
    public R<User> getUserInfoByPhone(String phone) {
        User user = query().eq("phone", phone).one();
        return R.ok(user);
    }

    /**
     * 电话号码创建用户
     *
     * @param phone 手机号
     * @return 用户
     */
    @Override
    public R<User> createUserByPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        System.out.println("用户信息为"+user);
        return R.ok(user);
    }

    /**
     * 根据用户id列表查询用户列表
     *
     * @param userIdList 用户id列表
     * @return 用户列表
     */
    @Override
    public R<List<User>> getUserList(List<Long> userIdList) {
        //根据用户id查询用户  where id in (5,2) order by field (id,5,2)
        String idStr = StrUtil.join(",",userIdList);
        List<User> userList = query().in("id", userIdList).last("order by field(id," + idStr + ")").list();
        userList = userList.stream().map(user -> {
            UserInfo userInfo = userInfoService.getByUserId(user.getId());
            if(userInfo != null){
                user.setIntroduce(userInfo.getIntroduce());
            }
            return user;
        }).collect(Collectors.toList());
        return R.ok(userList);
    }

    /**
     * 根据用户id查询用户
     *
     * @param id 用户id
     * @return 用户
     */
    @Override
    public User queryUserById(Long id) {
        User user = getById(id);
        if(user!= null){
            queryUserInfo(user);
            user.setIsFollow((Boolean) remoteFollowService.isFollowed(id).getData());
        }
        return (user);
    }

    /**
     * 查询用户info信息
     *
     * @param user 用户
     */
  private void  queryUserInfo(User user){
      UserInfo userInfo = userInfoService.getByUserId(user.getId());
      if (userInfo != null){
          user.setIntroduce(userInfo.getIntroduce());
          user.setCity(userInfo.getCity());
      }
    }
    /**
     * 获取用户统计信息
     *
     * @param userId 用户id
     * @return 用户统计信息
     */
    @Override
    public Stats getStats(Long userId) {
        //获取粉丝数
        Integer fanCount = (Integer) remoteFollowService.getFanCount(userId).getData();
        //获取关注数
        Integer followCount = (Integer) remoteFollowService.getFollowCount(userId).getData();
        // 当前用户id
        com.smartLive.common.core.domain.UserDTO user = UserContextHolder.getUser();
        Long currentUserId = null;
        Integer commonFollowCount= 0;
        if(user!= null){
            currentUserId = user.getId();
            //获取共同关注数
             commonFollowCount = (Integer) remoteFollowService.getCommonFollowCount(userId,currentUserId).getData();
        }
        //使用线程池＋future来实现
        //获取博客数
        Integer blogCount = remoteBlogService.getBlogCount(userId).getData();
        //获取点赞数
        Integer likeCount = remoteBlogService.getLikeCount(userId).getData();
        //获取发表评论数量
        Integer commentCount = remoteCommentService.getCommentCount(userId).getData();
        //获取订单数量
        Integer orderCount = remoteOrderService.getOrderCount(userId).getData();
        //获取收藏数量
        Integer collectCount = (Integer) remoteFollowService.getFollowShopCount(userId).getData();
        Stats stats= Stats.builder()
                .blogCount(blogCount)
                .followCount(followCount)
                .commonFollowCount(commonFollowCount)
                .fansCount(fanCount)
                .likeCount(likeCount)
                .commentCount(commentCount)
                .orderCount(orderCount)
                .collectCount(collectCount)
                .build();
        return stats;
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
                List<UserInfo> userInfos = userInfoService.listByUserIds(userIds);
                Map<Long,UserInfo> userInfoMap= userInfos.stream().collect(Collectors.toMap(UserInfo::getUserId, userInfo -> userInfo));
               users.forEach(
                       user -> {
                           UserInfo userInfo = userInfoMap.get(user.getId());
                           if(userInfo != null){
                               user.setIntroduce(userInfo.getIntroduce());
                               user.setCity(userInfo.getCity());
                           }
//                           queryUserInfo(user);
                       }
               );
               // 创建请求并发送
               EsBatchInsertRequest request = new EsBatchInsertRequest();
               request.setIndexName(EsIndexNameConstants.USER_INDEX_NAME);
               request.setData(users);
               request.setDataType(EsDataTypeConstants.USER);
               rabbitTemplate.convertAndSend(
                       MqConstants.ES_EXCHANGE,
                       MqConstants.ES_ROUTING_USER_BATCH_INSERT,
                       request
               );
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
        for (String id : ids) {
            executorService.submit(()->{
                log.info("线程：{}发布用户id：{}",Thread.currentThread().getName(), id);
                User user = query().eq("id", id).one();
                if (user== null){
                    return;
                }
                queryUserInfo(user);
                EsInsertRequest esInsertRequest = new EsInsertRequest();
                esInsertRequest.setIndexName(EsIndexNameConstants.USER_INDEX_NAME);
                esInsertRequest.setData(user);
                esInsertRequest.setId(user.getId());
                esInsertRequest.setDataType(EsDataTypeConstants.USER);
                rabbitTemplate.convertAndSend(MqConstants.ES_EXCHANGE, MqConstants.ES_ROUTING_USER_INSERT, esInsertRequest);
            });
        }
        return "发布成功";
    }
}
