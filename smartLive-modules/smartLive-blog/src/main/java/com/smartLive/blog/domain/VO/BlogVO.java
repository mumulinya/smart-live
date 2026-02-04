package com.smartLive.blog.domain.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 博客返回对象 (View Object)
 * 用于前端展示博客详情
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Data
public class BlogVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 商户id */
    private Long shopId;
    
    /** 博客类型id */
    private Long typeId;

    /** 用户id */
    private Long userId;

    /** 标题 */
    private String title;

    /** 探店的照片，最多9张，多张以","隔开 */
    private String images;

    /** 探店的文字描述 */
    private String content;

    /** 点赞数量 */
    private Integer liked;

    /** 收藏数量 */
    private Integer stared;

    /** 评论数量 */
    private Integer comments;
    
    /** 置顶 */
    private Boolean pin;
    
    /** 状态，0：正常，1：草稿，2：禁止查看 */
    private Short status;
    
    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

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
     * 是否收藏过了
     */
    private Boolean isStared;
}