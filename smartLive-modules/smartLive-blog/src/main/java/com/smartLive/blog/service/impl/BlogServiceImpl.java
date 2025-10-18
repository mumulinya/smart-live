package com.smartLive.blog.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.blog.domain.Blog;
import com.smartLive.blog.mapper.BlogMapper;
import com.smartLive.blog.service.IBlogService;
import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.domain.ScrollResult;
import com.smartLive.common.core.domain.UserDTO;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartLive.user.api.domain.BlogDTO;
import com.smartLive.user.api.domain.User;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 博客Service业务层处理
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService
{
    @Autowired
    private BlogMapper blogMapper;


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RemoteAppUserService remoteAppUserService;

    /**
     * 查询博客
     * 
     * @param id 博客主键
     * @return 博客
     */
    @Override
    public Blog selectBlogById(Long id)
    {
        return blogMapper.selectBlogById(id);
    }

    /**
     * 查询博客列表
     * 
     * @param blog 博客
     * @return 博客
     */
    @Override
    public List<Blog> selectBlogList(Blog blog)
    {
        return blogMapper.selectBlogList(blog);
    }

    /**
     * 新增博客
     * 
     * @param blog 博客
     * @return 结果
     */
    @Override
    public int insertBlog(Blog blog)
    {
        blog.setCreateTime(DateUtils.getNowDate());
        return blogMapper.insertBlog(blog);
    }

    /**
     * 修改博客
     * 
     * @param blog 博客
     * @return 结果
     */
    @Override
    public int updateBlog(Blog blog)
    {
        blog.setUpdateTime(DateUtils.getNowDate());
        return blogMapper.updateBlog(blog);
    }

    /**
     * 批量删除博客
     * 
     * @param ids 需要删除的博客主键
     * @return 结果
     */
    @Override
    public int deleteBlogByIds(Long[] ids)
    {
        return blogMapper.deleteBlogByIds(ids);
    }

    /**
     * 删除博客信息
     * 
     * @param id 博客主键
     * @return 结果
     */
    @Override
    public int deleteBlogById(Long id)
    {
        return blogMapper.deleteBlogById(id);
    }




    /**
     * 查询博客id查询博文详情
     *
     * @param id
     * @return
     */
    @Override
    public Result queryBlogById(Long id) {
        //从redis查询博客缓存
        String key= RedisConstants.CACHE_BLOG_KEY+id;
        String blogJson = stringRedisTemplate.opsForValue().get(key);
        if (blogJson != null) {
            //存在
            Blog blog = JSONUtil.toBean(blogJson, Blog.class);
            return Result.ok(blog);
        }
        //根据博客id查询博客信息
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("数据不存在");
        }
        // 查询blog有关的用户信息
        queryBlogUser(blog);
        //查询blog是否被点赞
        isBlogLiked(blog);
        //把博客信息存入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(blog));
        //返回结果
        return Result.ok(blog);
    }

    /**
     * 判断当前用户是否已经点赞
     * @param blog
     */
    private void isBlogLiked(Blog blog) {
        UserDTO user = UserContextHolder.getUser();
        if (user == null) {
            //未登录,不用查询是否点赞
            return;
        }
        //获取当前登录用户
        Long userId = user.getId();
        //判断当前用户是否已经点赞
        String key = RedisConstants.BLOG_LIKED_KEY + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score!=null);
    }

    /**
     * 查询最热博客
     *
     * @param current
     * @return
     */
    @Override

    public Result queryHotBlog(Integer current) {
        //从redis查询热门博客
        String key= RedisConstants.CACHE_HOT_BLOG_KEY+ current;
        String blogJson = stringRedisTemplate.opsForList().leftPop(key);
        if (blogJson != null) {
            //存在
            List<Blog> blogs = JSONUtil.toList(blogJson, Blog.class);
            return Result.ok(blogs);
        }

        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询blog有关的用户信息
        records.forEach(blog ->{
            queryBlogUser(blog);
            isBlogLiked(blog);
        });
        //把查询结果写入redis
        stringRedisTemplate.opsForList().leftPush(key, JSONUtil.toJsonStr(records));
        return Result.ok(records);
    }

    /**
     * 查询blog有关的用户信息
     * @param blog
     */
    private void queryBlogUser(Blog blog){
        Long userId = blog.getUserId();
        //根据用户id获取用户信息
        R<User> queryUserById = remoteAppUserService.queryUserById(userId);
        User user = queryUserById.getData();
        if (user != null) {
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
        }
    }

    /**
     * 点赞博客
     *
     * @param id
     * @return
     */
    @Override
    public Result likeBlog(Long id) {
        //获取当前登录用户
        UserDTO user = UserContextHolder.getUser();
        Long userId = user.getId();
        //判断当前用户是否已经点赞
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score!=null) {
            //已经点赞了,取消点赞
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            if (isSuccess) {
                //删除用户点赞信息
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }else{
            //未点赞
            //修改点赞数量
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            //保存用户点赞信息到redis的set集合 zadd key value score
            if (isSuccess) {
                //保存用户点赞信息
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        }
        //清空缓存
       flashRedisCache( id);
        return Result.ok("点赞成功");
    }

    /**
     * 查询博客点赞数
     *
     * @param id
     * @return
     */
    @Override
    public Result queryBlogLikes(Long id) {
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        //查询top5的点赞数 zrange key 0 4
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (top5 == null || top5.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        //解析其中的用户id
        List<Long> userIdList = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        //根据用户id查询用户  where id in (5,2) order by field (id,5,2)
        R<List<User>> userR = remoteAppUserService.getUserList(userIdList);
        if (userR.getCode() != 200) {
            return Result.fail(userR.getMsg());
        }
        List<User> userList = userR.getData();
        List<UserDTO> userDTOList = userList.stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());
        return Result.ok(userDTOList);
    }

    /**
     * 查询用户发布的博客
     *
     * @param current
     * @param userId
     * @return
     */
    @Override
    public Result queryBlogByUserId(Integer current, Long userId) {
        Page<Blog> page = query()
                .eq("user_id", userId)
                .orderByAsc("create_time")
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        List<Blog> records = page.getRecords();
        records.forEach(blog ->{
            isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    /**
     * 保存博客
     *
     * @param blog
     * @return
     */
    @Override
    public Result saveBlog(Blog blog) {
        blog.setUserId(UserContextHolder.getUser().getId());
        // 保存探店笔记
        boolean success = save(blog);
        if (!success) {
            return Result.fail("新增博文失败");
        }
        //推送笔记id给所有粉丝
        BlogDTO blogDTO = BeanUtil.copyProperties(blog, BlogDTO.class);
        //发送rabbitMq消息 推送笔记id给粉丝
        rabbitTemplate.convertAndSend(MqConstants.BLOG_EXCHANGE_NAME, MqConstants.BLOG_FEED_ROUTING, blogDTO);
        //返回id
        return Result.ok(blog.getId());
    }

    /**
     * 查询用户关注的用户发布的博客
     *
     * @param max
     * @param offset
     * @return
     */
    @Override
    public Result queryBlogByFollow(Long max, Integer offset) {
        //获取当前登录用户
        Long userId = UserContextHolder.getUser().getId();
        String key = RedisConstants.FEED_KEY + userId;
        //查询收件箱 关注的用户发布的博客
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, 0,max , offset, 2);
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        //获取博客id解析数据：blogId  minTime(时间戳) offset
        List<Long> blogIdList = new ArrayList<>(typedTuples.size());
        long minTime = 0L;
        int os = 1;
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            //获取博客id
            blogIdList.add(Long.valueOf(typedTuple.getValue()));
            Long time = typedTuple.getScore().longValue();
            if (minTime == time) {
                os++;
            }else {
                minTime = time;
                os = 1;
            }
        }

        //根据id查询博客
        String idsStr = StrUtil.join(",",blogIdList);
        List<Blog> blogList = query().in("id",blogIdList).last("order by field(id,"+idsStr+")").list();
        blogList.forEach(blog ->{
            //查询blog有关的用户信息
            queryBlogUser(blog);
            //查询blog是否被点赞
            isBlogLiked(blog);
        });
        //封装响应数据
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogList);
        scrollResult.setOffset(os);
        scrollResult.setMinTime(minTime);
        return Result.ok(scrollResult);
    }

    /**
     * 更新博客的评论数
     *
     * @param blogId
     * @return
     */
    @Override
    public R<Boolean> updateCommentById(Long blogId) {
        boolean update = update().setSql("comments = comments + 1").eq("id", blogId).update();
        //清空缓存
        flashRedisCache(blogId);
        return update ? R.ok() : R.fail();
    }

    /**
     * 查询我的博客
     *
     * @param current
     * @return
     */
    @Override
    public List<Blog> queryMyBlog(Integer current) {
        UserDTO user = UserContextHolder.getUser();
        // 根据用户查询
        Page<Blog> page = query()
                .eq("user_id", user.getId())
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> blogList = page.getRecords();
        blogList.forEach(blog ->{
            //查询blog有关的用户信息
            queryBlogUser(blog);
            //查询blog是否被点赞
            isBlogLiked(blog);
        });
        return blogList;
    }

    /**
     * 查询博客详情
     *
     * @param id
     * @return
     */
    @Override
    public R<Blog> getBlogById(Long id) {
        Blog blog = blogMapper.selectBlogById(id);
        return R.ok(blog);
    }

    /**
     * 查询用户博客数量
     *
     * @param userId
     * @return
     */
    @Override
    public Integer getBlogCount(Long userId) {
        //查询数量
        Long count = query().eq("user_id", userId).count();

        Integer blogCount = count.intValue(); // 直接转换，超出范围会截断
        return blogCount;
    }

    /**
     * 查询用户博客点赞数量
     *
     * @param userId
     * @return
     */
    @Override
    public Integer getLikeCount(Long userId) {
        List<Blog> blogList = query().eq("user_id", userId).list();
        Integer likeCount = blogList.stream().mapToInt(blog -> blog.getLiked()).sum();
        return likeCount;
    }

    /**
     * 刷新缓存
     *
     * @return
     */
    @Override
    public String flushCache() {
        stringRedisTemplate.delete(RedisConstants.CACHE_HOT_BLOG_KEY);
        return null;
    }

    /**
     * 清空缓存
     *
     * @param blogId
     */
    private void flashRedisCache(Long blogId) {
        ///清空缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_HOT_BLOG_KEY);
        //清空缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_BLOG_KEY+blogId);
    }
}
