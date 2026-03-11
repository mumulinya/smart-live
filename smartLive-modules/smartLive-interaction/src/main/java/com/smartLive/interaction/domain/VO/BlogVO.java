package com.smartLive.interaction.domain.VO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartLive.common.core.annotation.Excel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 博客摘要视图对象
 * 用于在 Feed 流或热榜中展示博客的基础信息及互动统计数据。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlogVO {
    /**
     *  主键
     */
    private Long id;
    /**
     * 用户id
     */
    private Long userId;
    /**
     *  标题
     */
    private String title;
    /**
     * 探店的照片，最多9张，多张以","隔开
     */
    private String images;
    /**
     *  探店的文字描述
     */
    private String content;
    /**
     *  点赞数量
     */
    private Integer liked;
    /**
     * 评论数量
     */
    private Integer comments;
    /**
     * 用户图标
     */
    private String icon;
    /**
     * 用户姓名
     */
    private String name;
    /**
     * 是否点赞过了
     */
    private Boolean isLike;
    /**
     * 数据类型
     */
    private String dataType;
    /**
     * 发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishTime;  // 发布时间

    /** 
     * 收藏数
     * 博客的长期价值指标，权重通常仅次于点赞。
     */
    private Integer stared;
}
