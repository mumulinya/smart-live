package com.smartLive.interaction.service.impl;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartLive.blog.api.RemoteBlogService;
import com.smartLive.blog.api.dto.BlogDto;
import com.smartLive.common.core.constant.MqConstants;
import com.smartLive.common.core.constant.RedisConstants;
import com.smartLive.common.core.constant.SystemConstants;
import com.smartLive.common.core.context.UserContextHolder;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.utils.DateUtils;
import com.smartLive.common.core.utils.rabbitMq.MqMessageSendUtils;
import com.smartLive.common.core.web.domain.Result;
import com.smartLive.interaction.domain.AIGenerateRequest;
import com.smartLive.interaction.domain.Comment;
import com.smartLive.interaction.domain.CommentDTO;
import com.smartLive.interaction.mapper.CommentMapper;
import com.smartLive.interaction.service.ICommentService;
import com.smartLive.shop.api.RemoteShopService;
import com.smartLive.shop.api.domain.ShopDTO;
import com.smartLive.user.api.RemoteAppUserService;
import com.smartLive.user.api.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

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

    @Autowired
    private RabbitTemplate rabbitTemplate;
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
                .orderByDesc("create_time")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Comment> list = page.getRecords();
        list.stream().forEach(c -> {
            Long id = c.getUserId();
            R<User> re = remoteAppUserService.queryUserById(id);
            User user = re.getData();
            if (user != null){
                c.setNickName(user.getNickName());
                c.setUserIcon(user.getIcon());
            }
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
            //更新博客评论数,发送rabbitMq消息
            log.info("发送rabbitMq消息给blog");
//            rabbitTemplate.convertAndSend(MqConstants.BLOG_EXCHANGE_NAME,MqConstants.BLOG_COMMENT_ROUTING,id);
            MqMessageSendUtils.sendMqMessage(rabbitTemplate,MqConstants.BLOG_EXCHANGE_NAME,MqConstants.BLOG_COMMENT_ROUTING,id);
//            remoteBlogService.updateCommentById(id);
        }else if(i > 0 && comment.getSourceType()==2){
            //更新店铺评论数
//            remoteShopService.updateCommentById(id);
            log.info("发送rabbitMq消息给shop");
//            rabbitTemplate.convertAndSend(MqConstants.SHOP_EXCHANGE_NAME,MqConstants.SHOP_COMMENT_ROUTING,id);
            MqMessageSendUtils.sendMqMessage(rabbitTemplate,MqConstants.SHOP_EXCHANGE_NAME,MqConstants.SHOP_COMMENT_ROUTING,id);
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
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
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
     * @param comments
     * @return
     */
    @Override
    public Result saveAiCreateComment(List<CommentDTO> comments) {
        if(comments.size()==0){
            return Result.fail("请传入数据");
        }
        //清空redis缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_AI_COMMENT_KEY);
        comments.forEach(commentDTO -> {
            String key = RedisConstants. CACHE_AI_COMMENT_KEY+commentDTO.getSourceType()+":"+commentDTO.getSourceId();
            stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(commentDTO));
            //设置过期时间
//            stringRedisTemplate.expire(key, RedisConstants.CACHE_AI_COMMENT_TTL, TimeUnit.MINUTES);
        });
        return Result.ok();
    }

    /**
     * 获取用户发表的评论数
     *
     * @param userId
     * @return
     */
    @Override
    public Integer getCommentCount(Long userId) {
        int commentCount = query().eq("user_id", userId).count().intValue();
        return commentCount;
    }

    /**
     * 获取评论总数
     *
     * @return
     */
    @Override
    public Integer getCommentTotal() {
        return query().count().intValue();
    }

    /**
     * 创建ai自动创建的评论
     *
     * @param
     * @return
     */
    @Override
    public void aiCreateComment() {
        // 获取有评论的博客和店铺的id（保持顺序的去重）
        List<AIGenerateRequest> list = query().list().stream()
                .collect(Collectors.groupingBy(
                        Comment::getSourceType,
                        Collectors.mapping(
                                Comment::getSourceId,
                                Collectors.collectingAndThen(
                                        Collectors.toCollection(LinkedHashSet::new),
                                        ArrayList::new
                                )
                        )
                ))
                .entrySet().stream()
                .map(entry -> new AIGenerateRequest(
                        entry.getKey(),
                        entry.getValue()
                ))
                .collect(Collectors.toList());
        //发送rabbitMq消息给ai服务
        rabbitTemplate.convertAndSend(MqConstants.AI_EXCHANGE_NAME,MqConstants.AI_COMMENT_ROUTING,list);
    }
}
