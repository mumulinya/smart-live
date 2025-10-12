package com.smartLive.user.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.user.api.domain.UserDTO;
import com.smartLive.user.domain.Follow;
import com.smartLive.user.domain.Stats;
import com.smartLive.user.domain.UserInfo;
import com.smartLive.user.service.IFollowService;
import com.smartLive.user.service.IUserInfoService;
import lombok.Builder;
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
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService
{
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private IFollowService followService;

    @Autowired
    private IUserInfoService userInfoService;

    @Autowired
    private RemoteBlogService remoteBlogService;

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
            System.out.println("进入");
            String tokenKey = UserContextHolder.getUser().getToken();
            User userById = getById(user.getId());
            UserDTO userDTO= BeanUtil.copyProperties(userById, UserDTO.class);
            //存储
            System.out.println("tokenKey为"+tokenKey);
            Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                    CopyOptions.create()
                            //忽略空值
                            .setIgnoreNullValue(true)
                            //把userDto字段值转为字符串
                            .setFieldValueEditor((fieldName, fieldValue) -> fieldValue == null ? "" : fieldValue.toString()));
            System.out.println("userMap为"+userMap);
            //更新之前的数据
            stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
            //设置token有效期
            stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
            UserContextHolder.removeUser();
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
        return userMapper.deleteUserByIds(ids);
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
            user.setIsFollow((Boolean) followService.isFollowed(id).getData());
        }
        return (user);
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
        Integer fansCount = followService.query().eq("follow_user_id", userId).count();
        //获取关注数
        Integer followCount = followService.query().eq("user_id", userId).count();
        //获取博客数
        Integer blogCount = remoteBlogService.getBlogCount(userId).getData();
        Stats stats= Stats.builder()
                .blogCount(blogCount)
                .followCount(followCount)
                .fansCount(fansCount)
                .likeCount(0)
                .build();
        return stats;
    }
}
