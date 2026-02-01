package com.smartLive.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.blog.domain.Blog;
import com.smartLive.common.core.domain.ScrollResult;
import java.util.List;
import java.util.Map;

/**
 * 博客Service接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface IBlogService extends IService<Blog>
{
    /**
     * 查询博客
     * 
     * @param id 博客主键
     * @return 博客
     */
     Blog selectBlogById(Long id);

    /**
     * 查询博客列表
     * 
     * @param blog 博客
     * @return 博客集合
     */
     List<Blog> selectBlogList(Blog blog);

    /**
     * 新增博客
     * 
     * @param blog 博客
     * @return 结果
     */
     int insertBlog(Blog blog);

    /**
     * 修改博客
     * 
     * @param blog 博客
     * @return 结果
     */
     int updateBlog(Blog blog);

    /**
     * 批量删除博客
     * 
     * @param ids 需要删除的博客主键集合
     * @return 结果
     */
     int deleteBlogByIds(Long[] ids);

    /**
     * 删除博客信息
     * 
     * @param id 博客主键
     * @return 结果
     */
     int deleteBlogById(Long id);

    /**
     * 查询博客id查询博文详情
     * @param id
     * @return
     */
    Blog queryBlogById(Long id);

    /**
     * 查询最热博客
     * @param current
     * @return
     */
    List<Blog> queryHotBlog(Integer current);

    /**
     * 点赞博客
     * @param id
     * @return
     */
    Boolean likeBlog(Long id);


    /**
     * 查询用户发布的博客
     * @param current
     * @param userId
     * @return
     */
    List<Blog> queryBlogByUserId(Integer current, Long userId);

    /**
     * 保存博客
     * @param blog
     * @return
     */
    Long saveBlog(Blog blog);

    /**
     * 查询用户关注的用户发布的博客
     * @param max
     * @param offset
     * @return
     */
    ScrollResult queryBlogByFollow(Long max, Integer offset);

    /**
     * 更新博客的评论数
     * @param blogId
     * @return
     */
    Boolean updateCommentById(Long blogId);

    /**
     * 查询我的博客
     * @param current
     * @return
     */
    List<Blog> queryMyBlog(Blog blog,Integer current);

/**
     * 查询博客详情
     * @param id
     * @return
     */
    Blog getBlogById(Long id);
    /**
     * 查询用户博客数量
     * @param userId
     * @return
     */

    Integer getBlogCount(Long userId);

    /**
     * 查询用户博客点赞数量
     * @param userId
     * @return
     */
    Integer getLikeCount(Long userId);

    /**
     * 刷新缓存
     * @return
     */
    String flashCache();

    /**
     * 查询分类下的博客
     * @param typeId
     * @param current
     * @return
     */
    List<Blog> queryBlogByCategory(Long typeId, Integer current);

    /**
     * 全部发布博客
     *
     * @return 全部发布结果
     */
    String allPublish();

    /**
     * 发布博客
     *
     * @param
     * @return 发布结果
     */
    String publish( String[] ids);
    /**
     * 获取博客总数
     * @return
     */
    Integer getBlogTotal();
    /**
     * 获取博客列表
     * @param sourceIdList
     * @return
     */
    List<Blog> getBlogListByIds(List<Long> sourceIdList);
    /**
     * 置顶博客
     *
     * @return 结果
     */
    boolean isPin(Blog blog);
    /**
     * 批量更新点赞数
     * @param updateMap
     * @return
     */
    Boolean updateLikeCountBatch(Map<Long, Integer> updateMap);
    /**
     * 批量更新评论数
     * @param updateMap
     * @return
     */
    Boolean updateCommentCountBatch(Map<Long, Integer> updateMap);
    /**
     * 批量更新收藏数
     * @param updateMap
     * @return
     */
    Boolean updateStarCountBatch(Map<Long, Integer> updateMap);
    /**
     * 获取博客点赞数
     * @param sourceId
     * @return
     */
    Integer getBlogLikeCount(Long sourceId);
}
