package com.smartLive.ai.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 评价视图对象。
 */
@Data
public class ReviewVO {

    // ===== 基础字段 =====
    private Long id;
    private Long userId;
    private Long shopId;
    private Long orderId;

    // ===== 来源相关字段 =====
    /** 来源类型：1=店铺 2=文章 3=团购 */
    private Integer sourceType;
    private String sourceName;
    private Long sourceId;

    // ===== 评价内容 =====
    private String content;
    private List<String> images;

    // ===== 评分信息 =====
    /** 总体评分 */
    private Integer score;
    /** 服务评分 */
    private Short serviceScore;
    /** 口味评分 */
    private Short tasteScore;
    /** 环境评分 */
    private Short envScore;

    // ===== 互动数据 =====
    private Integer liked;
    private Integer replyCount;
    private Integer stared;

    // ===== 状态字段 =====
    /** 状态：0=正常 1=被举报 2=禁止查看 */
    private Integer status;
    /** 是否匿名 */
    private Boolean isAnonymous;
    /** 是否AI生成 */
    private Boolean isAIGenerated;

    // ===== 用户信息 =====
    private String nickName;
    private String userIcon;

    // ===== 时间戳 =====
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    // ===== 用户交互状态（当前用户）=====
    private Boolean isLike;
    private Boolean isStared;

    // ===== 查询条件字段（用于 Service 层）=====
    /** 最低评分过滤 */
    private Integer minScore;
    /** 分页/限制返回数量 */
    private Integer limit;
    /** 排序类型：0=默认 1=热度 2=评分 3=最新 */
    private Integer sortType;

    // ===== 辅助字段 =====
    /** RAG相关性得分 */
    private Float relevanceScore;
    /** 是否是AI推荐 */
    private Boolean isAIRecommended;
}