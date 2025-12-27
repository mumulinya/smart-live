package com.smartLive.common.core.domain.blog;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 博客对象 tb_blog
 * 
 * @author mumulin
 * @date 2025-09-21
 */
@Data
public class Blog extends BaseEntity implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 商户id */
    @Excel(name = "商户id")
    private Long shopId;
    /** 博客类型id */
    private Long typeId;

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
    private String icon;
    /**
     * 用户姓名
     */
    private String name;
    /**
     * 是否点赞过了
     */
    private Boolean isLike;


}
