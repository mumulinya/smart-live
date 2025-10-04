package com.smartLive.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.blog.domain.Blog;
import com.smartLive.common.core.domain.R;
import com.smartLive.common.core.web.domain.Result;

import java.util.List;

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
    public Blog selectBlogById(Long id);

    /**
     * 查询博客列表
     * 
     * @param blog 博客
     * @return 博客集合
     */
    public List<Blog> selectBlogList(Blog blog);

    /**
     * 新增博客
     * 
     * @param blog 博客
     * @return 结果
     */
    public int insertBlog(Blog blog);

    /**
     * 修改博客
     * 
     * @param blog 博客
     * @return 结果
     */
    public int updateBlog(Blog blog);

    /**
     * 批量删除博客
     * 
     * @param ids 需要删除的博客主键集合
     * @return 结果
     */
    public int deleteBlogByIds(Long[] ids);

    /**
     * 删除博客信息
     * 
     * @param id 博客主键
     * @return 结果
     */
    public int deleteBlogById(Long id);

    /**
     * 查询博客id查询博文详情
     * @param id
     * @return
     */
    Result queryBlogById(Long id);

    /**
     * 查询最热博客
     * @param current
     * @return
     */
    Result queryHotBlog(Integer current);

    /**
     * 点赞博客
     * @param id
     * @return
     */
    Result likeBlog(Long id);

    /**
     * 查询博客点赞数
     * @param id
     * @return
     */
    Result queryBlogLikes(Long id);

    /**
     * 查询用户发布的博客
     * @param current
     * @param userId
     * @return
     */
    Result queryBlogByUserId(Integer current, Long userId);

    /**
     * 保存博客
     * @param blog
     * @return
     */
    Result saveBlog(Blog blog);

    /**
     * 查询用户关注的用户发布的博客
     * @param max
     * @param offset
     * @return
     */
    Result queryBlogByFollow(Long max, Integer offset);

    /**
     * 更新博客的评论数
     * @param blogId
     * @return
     */
    R<Boolean> updateCommentById(Long blogId);
}
