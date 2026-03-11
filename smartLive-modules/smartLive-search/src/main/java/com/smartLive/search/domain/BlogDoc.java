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
 * 博客/笔记索引文档对象
 * 对应 ES 中的 blogs 索引，用于全文检索探店笔记与用户动态。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlogDoc extends BaseEntity implements Serializable
{
    private static final long serialVersionUID = 1L;
    
    /** 博客唯一标识 */
    private Long id;

    /** 博客分类 ID (与业务数据库对应) */
    private Long typeId;

    /** 笔记标题 (支持全文检索) */
    @Excel(name = "标题")
    private String title;

    /** 笔记配图 URL 列表 (逗号分隔存储) */
    private String images;

    /** 笔记正文内容 (核心搜索字段) */
    private String content;

    /** 累计点赞数 */
    private Integer liked;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 发布者头像 */
    private String icon;
    
    /** 发布者昵称 */
    private String name;

    /** 数据操作类型 (insert/update/delete) */
    private String actionType;
    
    /** 业务来源类型 (用于数据同步识别) */
    private Integer sourceType;
    
    /** 原始业务数据 ID */
    private Long sourceId;
}
