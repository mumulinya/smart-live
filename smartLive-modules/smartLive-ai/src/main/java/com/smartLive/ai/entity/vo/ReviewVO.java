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
    /** 评价编号 */
    private Long id;
    /** 用户编号 */
    private Long userId;
    /** 店铺编号 */
    private Long shopId;
    /** 订单编号 */
    private Long orderId;

    // ===== 来源相关字段 =====
    /** 来源类型：1=店铺 2=文章 3=团购 */
    private Integer sourceType;
    /** 来源名称 */
    private String sourceName;
    /** 来源编号 */
    private Long sourceId;

    // ===== 评价内容 =====
    /** 评价内容 */
    private String content;
    /** 评价图片列表 */
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
    /** 点赞数量 */
    private Integer liked;
    /** 回复数量 */
    private Integer replyCount;
    /** 收藏数量 */
    private Integer stared;

    // ===== 状态字段 =====
    /** 状态：0=正常 1=被举报 2=禁止查看 */
    private Integer status;
    /** 是否匿名 */
    private Boolean isAnonymous;
    /** 是否人工智能生成 */
    private Boolean isAIGenerated;

    // ===== 用户信息 =====
    /** 用户昵称 */
    private String nickName;
    /** 用户头像 */
    private String userIcon;

    // ===== 时间戳 =====
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    // ===== 用户交互状态（当前用户）=====
    /** 当前用户是否点赞 */
    private Boolean isLike;
    /** 当前用户是否收藏 */
    private Boolean isStared;

    // ===== 查询条件字段（用于服务层）=====
    /** 最低评分过滤 */
    private Integer minScore;
    /** 分页/限制返回数量 */
    private Integer limit;
    /** 排序类型：0=默认 1=热度 2=评分 3=最新 */
    private Integer sortType;

    // ===== 辅助字段 =====
    /** 检索增强生成相关性得分 */
    private Float relevanceScore;
    /** 是否为人工智能推荐 */
    private Boolean isAIRecommended;
}
