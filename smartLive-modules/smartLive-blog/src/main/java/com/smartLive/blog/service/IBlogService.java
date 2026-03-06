package com.smartLive.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartLive.blog.domain.Blog;
import com.smartLive.blog.domain.VO.BlogVO;
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
     * 根据博客ID查询博文详情
     *
     * @param id 博客主键
     * @return 博客详情VO
     */
    BlogVO queryBlogById(Long id);
    /**
     * 保存博客（发布/草稿）
     *
     * @param blog 博客实体
     * @return 博客ID
     */
    Long saveBlog(Blog blog);

    /**
     * 查询最热博客列表
     *
     * @param current 当前页码
     * @return 热门博客列表
     */
    List<BlogVO> queryHotBlog(Integer current);

    /**
     * 查询指定用户发布的博客列表
     *
     * @param current 当前页码
     * @param userId  用户ID
     * @return 博客列表
     */
    List<BlogVO> queryBlogByUserId(Integer current, Long userId);


    /**
     * 查询我的博客列表
     *
     * @param blog    博客查询条件（包含状态等筛选参数）
     * @param current 当前页码
     * @return 我的博客列表
     */
    List<BlogVO> queryMyBlog(Blog blog, Integer current);

    /**
     * 查询博客详情（包含用户信息）
     *
     * @param id 博客主键
     * @return 博客详情VO
     */
    BlogVO getBlogById(Long id);



    /**
     * 查询指定分类下的博客列表
     *
     * @param typeId  分类ID
     * @param current 当前页码
     * @return 博客列表
     */
    List<BlogVO> queryBlogByCategory(Long typeId, Integer current);

    /**
     * 根据ID列表批量获取博客（含用户信息、点赞状态）
     *
     * @param sourceIdList 博客ID列表
     * @return 博客列表
     */
    List<Blog> getBlogListByIds(List<Long> sourceIdList);
    /**
     * 置顶/取消置顶博客
     *
     * @param blog 博客实体（包含ID和置顶状态）
     * @return 操作结果
     */
    boolean isPin(Blog blog);
    /**
     * 批量更新博客点赞数
     *
     * @param updateMap 博客ID与点赞数的映射
     * @return 更新结果
     */
    Boolean updateLikeCountBatch(Map<Long, Integer> updateMap);
    /**
     * 批量更新博客评论数
     *
     * @param updateMap 博客ID与评论数的映射
     * @return 更新结果
     */
    Boolean updateCommentCountBatch(Map<Long, Integer> updateMap);
    /**
     * 批量更新博客收藏数
     *
     * @param updateMap 博客ID与收藏数的映射
     * @return 更新结果
     */
    Boolean updateStarCountBatch(Map<Long, Integer> updateMap);
    /**
     * 全部发布博客
     *
     * @return 全部发布结果
     */
    String allPublish();

    /**
     * 批量发布博客至ES索引
     *
     * @param ids 博客ID数组
     * @return 发布结果
     */
    String publish(String[] ids);
    /**
     * 获取博客点赞数
     *
     * @param sourceId 博客ID
     * @return 点赞数量
     */
    Integer getBlogLikeCount(Long sourceId);
    /**
     * 获取博客收藏数
     *
     * @param sourceId 博客ID
     * @return 收藏数量
     */
    Integer getBlogStarCount(Long sourceId);
    /**
     * 获取博客总数
     *
     * @return 博客总数
     */
    Integer getBlogTotal();
    /**
     * 查询用户博客数量
     *
     * @param userId 用户ID
     * @return 博客数量
     */
    Integer getBlogCount(Long userId);

    /**
     * 查询用户博客获得的总点赞数
     *
     * @param userId 用户ID
     * @return 点赞总数
     */
    Integer getLikeCount(Long userId);
    /**
     * 刷新博客缓存（详情 + 列表 + 分类）
     *
     * @return 刷新结果
     */
    String flashCache();
    /**
     * 更新博客状态（审核通过/拒绝）
     *
     * @param targetId 博客ID
     * @param status   博客状态
     * @param reason   拒绝原因（通过时为null）
     * @return 更新结果
     */
    Boolean updateBlogStatus(Long targetId, Integer status, String reason);
}
