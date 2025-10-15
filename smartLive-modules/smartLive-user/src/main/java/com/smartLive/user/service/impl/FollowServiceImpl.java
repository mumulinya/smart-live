package com.smartLive.user.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.blog.domain.Blog;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartLive.user.api.domain.BlogDTO;
import com.smartLive.user.api.domain.User;
import com.smartLive.user.api.domain.UserDTO;
import com.smartLive.user.domain.Follow;
import com.smartLive.user.mapper.FollowMapper;
import com.smartLive.user.service.IFollowService;
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
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService
{
    @Autowired
    private FollowMapper followMapper;


    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RemoteAppUserService remoteAppUserService;
    /**
     * 查询关注
     * 
     * @param id 关注主键
     * @return 关注
     */
    @Override
    public Follow selectFollowById(Long id)
    {
        return followMapper.selectFollowById(id);
    }

    /**
     * 查询关注列表
     * 
     * @param follow 关注
     * @return 关注
     */
    @Override
    public List<Follow> selectFollowList(Follow follow)
    {
        return followMapper.selectFollowList(follow);
    }

    /**
     * 新增关注
     * 
     * @param follow 关注
     * @return 结果
     */
    @Override
    public int insertFollow(Follow follow)
    {
        follow.setCreateTime(DateUtils.getNowDate());
        return followMapper.insertFollow(follow);
    }

    /**
     * 修改关注
     * 
     * @param follow 关注
     * @return 结果
     */
    @Override
    public int updateFollow(Follow follow)
    {
        return followMapper.updateFollow(follow);
    }

    /**
     * 批量删除关注
     * 
     * @param ids 需要删除的关注主键
     * @return 结果
     */
    @Override
    public int deleteFollowByIds(Long[] ids)
    {
        return followMapper.deleteFollowByIds(ids);
    }

    /**
     * 删除关注信息
     * 
     * @param id 关注主键
     * @return 结果
     */
    @Override
    public int deleteFollowById(Long id)
    {
        return followMapper.deleteFollowById(id);
    }


    /**
     * 关注或取关
     *
     * @param followUserId
     * @param isFollow
     * @return
     */
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        //获取当前用户id
        Long userId = UserContextHolder.getUser().getId();
        String key = RedisConstants.FOLLOW_USER_KEY + userId;
        //判断是关注还是取关
        if(isFollow){
            //关注
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean save = save(follow);
            if (save) {
                //关注成功，添加关注到redis
                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            }
        }else{
            //取关
            boolean remove = remove(new QueryWrapper<Follow>().eq("user_id", userId).eq("follow_user_id", followUserId));
            if (remove) {
                //取关成功，从redis中删除关注
                stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
            }
        }
        return Result.ok();
    }

    /**
     * 判断是否关注
     *
     * @param followUserId
     * @return
     */
    @Override
    public Result isFollowed(Long followUserId) {
        //获取当前用户id
        Long userId = UserContextHolder.getUser().getId();
        String key = RedisConstants.FOLLOW_USER_KEY + userId;
//        //判断是否关注 从数据库中查询
//        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        //判断是否关注 从redis的set集合中查询
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, followUserId.toString());
        return Result.ok(isMember);
    }

    /**
     * 共同关注
     *
     * @param userId
     * @return
     */
    @Override
    public Result common(Long userId) {
        //获取当前用户id
        Long currentUserId = UserContextHolder.getUser().getId();
        String key = RedisConstants.FOLLOW_USER_KEY + currentUserId;
        String key2 = RedisConstants.FOLLOW_USER_KEY + userId;
        //查询共同关注
        Set<String> common = stringRedisTemplate.opsForSet().intersect(key, key2);
        if (common == null || common.isEmpty()) {
            //没有共同关注
            return Result.ok(Collections.emptyList());
        }
        //将字符串转换为Long类型
        List<Long> idList = common.stream().map(Long::valueOf).collect(Collectors.toList());
        //根据id查询用户
        R<List<User>> userSuccess = remoteAppUserService.getUserList(idList);
        if (userSuccess.getCode() != 200) {
            return Result.fail(userSuccess.getMsg());
        }
        List<User> userList = userSuccess.getData();
        userList.forEach(user -> {
            user.setIsFollow((Boolean) isFollowed(user.getId()).getData());
        });
        List<UserDTO> userDTOList = userList.stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());
        return Result.ok(userDTOList);
    }

    /**
     * 发送博客给关注者
     *
     * @param blogDTO
     */
    @Override
    public void sendBlogToFollowers(BlogDTO blogDTO) {
        //推送笔记id给所有粉丝
        // 查询笔记作者下的所有粉丝
        List<Follow> followList = query().eq("follow_user_id", blogDTO.getUserId()).list();
        for (Follow follow : followList) {
            //获取粉丝id
            Long userId = follow.getUserId();
            //推送
            String key = RedisConstants.FEED_KEY + userId;
            stringRedisTemplate.opsForZSet().add(key, blogDTO.getId().toString(), System.currentTimeMillis());
        }
    }

    /**
     * 获取粉丝列表
     *
     * @return
     */
    @Override
    public Result getFans(Long followUserId,Integer current) {
        //获取粉丝id
        List<Long> userIdList = query()
                .select("user_id")
                .eq("follow_user_id", followUserId)
                .orderByDesc("create_time") // 添加排序
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE))
                .getRecords()                .stream()
                .map(Follow::getUserId)  // 假设 follow 是你的实体类
                .collect(Collectors.toList());
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
            user.setIsFollow((Boolean) isFollowed(user.getId()).getData());
        });
        return Result.ok(userList);
    }

    /**
     * 获取关注列表
     *
     * @param userId
     * @return
     */
    @Override
    public Result getFollows(Long userId,Integer current) {
        List<Long> followUserIdList = query()
                .select("follow_user_id")
                .eq("user_id", userId)
                .orderByDesc("create_time") // 添加排序
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE))
                .getRecords()
                .stream()
                .map(Follow::getFollowUserId)  // 假设 follow 是你的实体类
                .collect(Collectors.toList());
                if(followUserIdList.isEmpty()){
                    return Result.ok(Collections.emptyList());
                }
                R<List<User>> userSuccess = remoteAppUserService.getUserList(followUserIdList);
                if (userSuccess.getCode() != 200) {
                    return Result.fail(userSuccess.getMsg());
                }
                List<User> userList = userSuccess.getData();
        return Result.ok(userList);
    }
}
