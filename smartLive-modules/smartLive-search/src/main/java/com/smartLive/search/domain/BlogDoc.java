package com.smartLive.search.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 博客文档对象
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)  // 忽略未知字段
public class BlogDoc extends BaseEntity implements Serializable
{
    private static final long serialVersionUID = 1L;
    private Long id;

    /** 博客类型id */
    private Long typeId;

    /** 标题 */
    @Excel(name = "标题")
    private String title;

    /** 探店的照片，最多9张，多张以","隔开 */
    private String images;

    /** 探店的文字描述 */
    private String content;

    /** 点赞数量 */
    private Integer liked;

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

    private String actionType;
    private Integer sourceType;
    private Long sourceId;
}