package com.smartLive.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartLive.interaction.domain.Review;
import com.smartLive.interaction.domain.VO.BadReviewVO;
import com.smartLive.interaction.domain.VO.ShopReviewAnalysisVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ReviewMapper extends BaseMapper<Review> {
    Review selectReviewById(Long id);

    List<Review> selectReviewList(Review review);

    int insertReview(Review review);

    int updateReview(Review review);

    int deleteReviewById(Long id);

    int deleteReviewByIds(Long[] ids);

    ShopReviewAnalysisVO selectShopReviewAnalysis(@Param("shopId") Long shopId,
                                                  @Param("auditStatus") Integer auditStatus,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);

    List<BadReviewVO> selectBadReviewList(@Param("shopId") Long shopId,
                                          @Param("auditStatus") Integer auditStatus,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime,
                                          @Param("minScore") Integer minScore,
                                          @Param("maxScore") Integer maxScore,
                                          @Param("limit") Integer limit);

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