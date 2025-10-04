package com.smartLive.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.blog.domain.Blog;

import java.util.List;


/**
 * 博客Mapper接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface BlogMapper  extends BaseMapper<Blog>
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
     * 删除博客
     * 
     * @param id 博客主键
     * @return 结果
     */
    public int deleteBlogById(Long id);

    /**
     * 批量删除博客
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteBlogByIds(Long[] ids);
}
