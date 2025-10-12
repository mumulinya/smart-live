package com.smartLive.blog.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.Date;

/**
 * 博客对象 tb_blog
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@TableName("tb_blog")
@Data
public class Blog extends BaseEntity implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商户id */
    @Excel(name = "商户id")
    private Long shopId;

    /** 用户id */
    @Excel(name = "用户id")
    private Long userId;

    /** 标题 */
    @Excel(name = "标题")
    private String title;

    /** 探店的照片，最多9张，多张以","隔开 */
    @Excel(name = "探店的照片，最多9张，多张以','隔开")
    private String images;

    /** 探店的文字描述 */
    @Excel(name = "探店的文字描述")
    private String content;

    /** 点赞数量 */
    @Excel(name = "点赞数量")
    private Integer liked;

    /** 评论数量 */
    @Excel(name = "评论数量")
    private Integer comments;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 用户图标
     */
    @TableField(exist = false)
    private String icon;
    /**
     * 用户姓名
     */
    @TableField(exist = false)
    private String name;
    /**
     * 是否点赞过了
     */
    @TableField(exist = false)
    private Boolean isLike;


}
