package com.smartLive.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.blog.domain.Blog;
import com.smartLive.blog.domain.VO.BlogVO;
import com.smartLive.common.core.domain.ScrollResult;
import java.util.List;
import java.util.Map;

/**
 * 博客服务接口。
 *
 * @author mumulin
 * @date 2025-09-21
 */
public interface IBlogService extends IService<Blog>
{
    /**
     * 根据博客ID查询博客实体。
     *
     * @param id 博客ID
     * @return 博客实体
     */
    Blog selectBlogById(Long id);

    /**
     * 查询博客实体列表。
     *
     * @param blog 查询条件
     * @return 博客实体列表
     */
    List<Blog> selectBlogList(Blog blog);

    /**
     * 根据博客ID查询博客视图。
     *
     * @param id 博客ID
     * @return 博客视图
     */
    BlogVO selectBlogVoById(Long id);

    /**
     * 查询博客视图列表。
     *
     * @param blog 查询条件
     * @return 博客视图列表
     */
    List<BlogVO> selectBlogVoList(Blog blog);

    /**
     * 新增博客。
     *
     * @param blog 博客信息
     * @return 影响行数
     */
    int insertBlog(Blog blog);

    /**
     * 修改博客。
     *
     * @param blog 博客信息
     * @return 影响行数
     */
    int updateBlog(Blog blog);

    /**
     * 批量删除博客。
     *
     * @param ids 博客ID数组
     * @return 影响行数
     */
    int deleteBlogByIds(Long[] ids);

    /**
     * 根据博客ID删除博客。
     *
     * @param id 博客ID
     * @return 影响行数
     */
    int deleteBlogById(Long id);

    /**
     * 查询博客详情。
     *
     * @param id 博客ID
     * @return 博客详情
     */
    BlogVO queryBlogById(Long id);

    /**
     * 保存博客内容。
     *
     * @param blog 博客信息
     * @return 博客ID
     */
    Long saveBlog(Blog blog);

    /**
     * 分页查询热门博客。
     *
     * @param current 当前页
     * @return 热门博客列表
     */
    List<BlogVO> queryHotBlog(Integer current);

    /**
     * 查询指定用户发布的博客。
     *
     * @param current 当前页
     * @param userId 用户ID
     * @return 博客列表
     */
    List<BlogVO> queryBlogByUserId(Integer current, Long userId);

    /**
     * 查询当前用户的博客列表。
     *
     * @param blog 查询条件
     * @param current 当前页
     * @return 博客列表
     */
    List<BlogVO> queryMyBlog(Blog blog, Integer current);

    /**
     * 获取博客详情页数据。
     *
     * @param id 博客ID
     * @return 博客详情
     */
    BlogVO getBlogById(Long id);

    /**
     * 根据博客分类查询博客列表。
     *
     * @param typeId 分类ID
     * @param current 当前页
     * @return 博客列表
     */
    List<BlogVO> queryBlogByCategory(Long typeId, Integer current);

    /**
     * 根据博客ID列表批量查询博客。
     *
     * @param sourceIdList 博客ID列表
     * @return 博客列表
     */
    List<BlogVO> getBlogListByIds(List<Long> sourceIdList);

    /**
     * 判断博客是否置顶。
     *
     * @param blog 博客信息
     * @return 是否置顶
     */
    boolean isPin(Blog blog);

    /**
     * 批量更新博客点赞数。
     *
     * @param updateMap 博客ID与点赞数映射
     * @return 更新结果
     */
    Boolean updateLikeCountBatch(Map<Long, Integer> updateMap);

    /**
     * 批量更新博客评论数。
     *
     * @param updateMap 博客ID与评论数映射
     * @return 更新结果
     */
    Boolean updateCommentCountBatch(Map<Long, Integer> updateMap);

    /**
     * 批量更新博客收藏数。
     *
     * @param updateMap 博客ID与收藏数映射
     * @return 更新结果
     */
    Boolean updateStarCountBatch(Map<Long, Integer> updateMap);

    /**
     * 全量发布博客数据到搜索索引。
     *
     * @return 执行结果
     */
    String allPublish();

    /**
     * 发布指定博客数据到搜索索引。
     *
     * @param ids 博客ID数组
     * @return 执行结果
     */
    String publish(String[] ids);

    /**
     * 获取博客点赞数。
     *
     * @param sourceId 博客ID
     * @return 点赞数
     */
    Integer getBlogLikeCount(Long sourceId);

    /**
     * 获取博客收藏数。
     *
     * @param sourceId 博客ID
     * @return 收藏数
     */
    Integer getBlogStarCount(Long sourceId);

    /**
     * 获取博客总数。
     *
     * @return 博客总数
     */
    Integer getBlogTotal();

    /**
     * 获取用户发布的博客数量。
     *
     * @param userId 用户ID
     * @return 博客数量
     */
    Integer getBlogCount(Long userId);

    /**
     * 获取用户博客获赞总数。
     *
     * @param userId 用户ID
     * @return 获赞总数
     */
    Integer getLikeCount(Long userId);

    /**
     * 刷新博客缓存。
     *
     * @return 执行结果
     */
    String flashCache();

    /**
     * 更新博客审核状态。
     *
     * @param targetId 博客ID
     * @param status 状态值
     * @param reason 审核原因
     * @return 更新结果
     */
    Boolean updateBlogStatus(Long targetId, Integer status, String reason);

    /**
     * 按标题模糊搜索博客列表。
     *
     * @param keyword 搜索关键词
     * @return 博客列表
     */
    List<BlogVO> searchBlogs(String keyword);
}