package com.smartLive.comment.service.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.blog.api.dto.BlogDto;
import com.smartLive.comment.domain.CommentDTO;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.domain.ShopDTO;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartLive.user.api.domain.BlogDTO;
import com.smartLive.user.api.domain.User;
import io.swagger.v3.core.util.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.smartLive.comment.mapper.CommentMapper;
import com.smartLive.comment.domain.Comment;
import com.smartLive.comment.service.ICommentService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 评论Service业务层处理
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Service
@Slf4j
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements ICommentService
{
    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private RemoteAppUserService remoteAppUserService;

    @Autowired
    private RemoteBlogService remoteBlogService;

    @Autowired
    private RemoteShopService remoteShopService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 查询评论
     * 
     * @param id 评论主键
     * @return 评论
     */
    @Override
    public Comment selectCommentById(Long id)
    {
        return commentMapper.selectCommentById(id);
    }

    /**
     * 查询评论列表
     * 
     * @param comment 评论
     * @return 评论
     */
    @Override
    public List<Comment> selectCommentList(Comment comment)
    {
        return commentMapper.selectCommentList(comment);
    }

    /**
     * 新增评论
     * 
     * @param comment 评论
     * @return 结果
     */
    @Override
    public int insertComment(Comment comment)
    {
        comment.setCreateTime(DateUtils.getNowDate());
        return commentMapper.insertComment(comment);
    }

    /**
     * 修改评论
     * 
     * @param comment 评论
     * @return 结果
     */
    @Override
    public int updateComment(Comment comment)
    {
        comment.setUpdateTime(DateUtils.getNowDate());
        return commentMapper.updateComment(comment);
    }

    /**
     * 批量删除评论
     * 
     * @param ids 需要删除的评论主键
     * @return 结果
     */
    @Override
    public int deleteCommentByIds(Long[] ids)
    {
        return commentMapper.deleteCommentByIds(ids);
    }

    /**
     * 删除评论信息
     * 
     * @param id 评论主键
     * @return 结果
     */
    @Override
    public int deleteCommentById(Long id)
    {
        return commentMapper.deleteCommentById(id);
    }

    /**
     * 获取评论列表
     *
     * @param comment
     * @return
     */
    @Override
    public Result listComment(Comment comment,Integer current) {
        Page<Comment> page = query().eq("source_id", comment.getSourceId())
                .eq("source_type", comment.getSourceType())
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Comment> list = page.getRecords();
        list.stream().forEach(c -> {
            Long id = c.getUserId();
            R<User> user = remoteAppUserService.queryUserById(id);
            c.setNickName(user.getData().getNickName());
            c.setUserIcon(user.getData().getIcon());
        });
        if(list.size()==0){
            return Result.ok(list);
        }
        //获取是否有ai生成评论
        String key = RedisConstants.CACHE_AI_COMMENT_KEY + comment.getSourceType() +":"+ comment.getSourceId();
        String JsonStr = stringRedisTemplate.opsForValue().get(key);
        if(JsonStr != null){
            Comment commentDTO = JSON.parseObject(JsonStr, Comment.class);
            list.add(commentDTO);
        }
        return Result.ok(list);
    }

    /**
     * 新增评论
     *
     * @param comment
     * @return
     */
    @Override
    @Transactional
    public Result addComment(Comment comment) {
        comment.setCreateTime(DateUtils.getNowDate());
        int i = commentMapper.insertComment(comment);
        Long id = comment.getSourceId();
        if(i > 0&& comment.getSourceType()==1){
            //更新博客评论数
            remoteBlogService.updateCommentById(id);
        }else if(i > 0 && comment.getSourceType()==2){
            //更新店铺评论数
            remoteShopService.updateCommentById(id);
        }
        return Result.ok(i);
    }

    /**
     * 获取我的评论
     *
     * @param current
     * @return
     */
    @Override
    public Result getCommentOfMe(Integer current) {
        Long userId = UserContextHolder.getUser().getId();
        Page<Comment> page = query().eq("user_id", userId)
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
                List<Comment> list = page.getRecords();
        return Result.ok(list);
    }

    /**
     * 获取评论列表
     *
     * @return
     */
    @Override
    public List<Comment> getCommentList() {
        List<Comment> list = query().list();
        list.stream().forEach(c -> {
            if(c.getSourceType()==1){
                R<BlogDto> blog = remoteBlogService.getBlogById(c.getSourceId());
                if((blog.getData().getTitle()!=null))
                c.setSourceName(blog.getData().getTitle());
            }else if(c.getSourceType()==2){
                R<ShopDTO> shop = remoteShopService.getShopById(c.getSourceId());
                if((shop.getData().getName()!=null))
                c.setSourceName(shop.getData().getName());
            }
        });
        return list;
    }

    /**
     * 保存ai自动创建的评论
     *
     * @param commentDTO
     * @return
     */
    @Override
    public Result saveAiCreateComment(CommentDTO commentDTO) {
        String key = RedisConstants. CACHE_AI_COMMENT_KEY+commentDTO.getSourceType()+":"+commentDTO.getSourceId();
        stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(commentDTO));
        stringRedisTemplate.expire(key, RedisConstants.CACHE_AI_COMMENT_TTL, TimeUnit.MINUTES);
        return Result.ok();
    }
}
