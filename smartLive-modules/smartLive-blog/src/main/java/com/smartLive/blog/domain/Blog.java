package com.smartLive.blog.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 博客实体类
 * 对应数据库表 tb_blog，记录了用户的探店笔记、媒体附件及互动统计数据。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@TableName("blog")
@Data
public class Blog extends BaseEntity implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商户ID */
    @Excel(name = "商户id")
    private Long shopId;
    /** 博客类型ID */
    private Long typeId;

    /** 用户ID */
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

    /** 收藏数量 */
    @Excel(name = "收藏数量")
    private Integer stared;

    /** 评论数量 */
    @Excel(name = "评论数量")
    private Integer comments;
    /** 置顶 */
    @Excel(name = "置顶")
    private Boolean pin;
    /** 状态，0：正常，1：草稿，2：禁止查看 */
    @Excel(name = "状态，0：正常，1：被举报，2：禁止查看")
    private Short status;

    /** 审核状态 */
    private Short auditStatus;

    /** 审核拒绝原因 */
    private String rejectReason;
    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
