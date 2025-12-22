package com.smartLive.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.blog.domain.Blog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;


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
    /**
     * 纯注解方式实现高效批量更新
     * 原理：拼接 UPDATE ... CASE WHEN ... SQL
     */
    @Update("<script>" +
            "UPDATE blog " +
            "SET liked = CASE id " +
            "  <foreach collection='map.entrySet()' index='key' item='val'> " +
            "    WHEN #{key} THEN #{val} " +
            "  </foreach> " +
            "END " +
            "WHERE id IN " +
            "  <foreach collection='map.keySet()' item='key' open='(' separator=',' close=')'> " +
            "    #{key} " +
            "  </foreach>" +
            "</script>")
    void updateLikeCountBatch(@Param("map") Map<Long, Integer> updateMap);
}
