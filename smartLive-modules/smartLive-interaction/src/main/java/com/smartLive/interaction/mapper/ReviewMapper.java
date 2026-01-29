package com.smartLive.interaction.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.interaction.domain.Review;
import com.smartLive.interaction.domain.Review;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 评论Mapper接口
 * 
 * @author mumulin
 * @date 2025-09-21
 */
public interface ReviewMapper extends BaseMapper<Review>
{
    /**
     * 查询评论
     * 
     * @param id 评论主键
     * @return 评论
     */
    public Review selectReviewById(Long id);

    /**
     * 查询评论列表
     * 
     * @param review 评论
     * @return 评论集合
     */
    public List<Review> selectReviewList(Review review);

    /**
     * 新增评论
     * 
     * @param review 评论
     * @return 结果
     */
    public int insertReview(Review review);

    /**
     * 修改评论
     * 
     * @param review 评论
     * @return 结果
     */
    public int updateReview(Review review);

    /**
     * 删除评论
     * 
     * @param id 评论主键
     * @return 结果
     */
    public int deleteReviewById(Long id);

    /**
     * 批量删除评论
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteReviewByIds(Long[] ids);

    @Update("<script>" +
            "UPDATE review " +
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
    @Update("<script>" +
            "UPDATE review " +
            "SET reply_count = CASE id " +
            "  <foreach collection='map.entrySet()' index='key' item='val'> " +
            "    WHEN #{key} THEN #{val} " +
            "  </foreach> " +
            "END " +
            "WHERE id IN " +
            "  <foreach collection='map.keySet()' item='key' open='(' separator=',' close=')'> " +
            "    #{key} " +
            "  </foreach>" +
            "</script>")
    void updateCommentCountBatch(@Param("map") Map<Long, Integer> batchMap);
    @Update("<script>" +
            "UPDATE review " +
            "SET stared = CASE id " +
            "  <foreach collection='map.entrySet()' index='key' item='val'> " +
            "    WHEN #{key} THEN #{val} " +
            "  </foreach> " +
            "END " +
            "WHERE id IN " +
            "  <foreach collection='map.keySet()' item='key' open='(' separator=',' close=')'> " +
            "    #{key} " +
            "  </foreach>" +
            "</script>")
    void updateStarCountBatch(@Param("map") Map<Long, Integer> batchMap);
}
