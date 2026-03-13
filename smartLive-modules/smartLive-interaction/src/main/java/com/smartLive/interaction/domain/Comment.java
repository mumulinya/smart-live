package com.smartLive.interaction.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 评论实体对象
 * 用于存储针对各类资源的一级回复及多级追评。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@TableName("comment")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Comment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户id */
    @Excel(name = "用户id")
    private Long userId;

    /** 来源类型  1（店铺）, 2（文章）, 3（团购）等。 */
    @Excel(name = "来源类型  1", readConverterExp = "店=铺")
    private Integer sourceType;

    /** 来源id  对应来源类型表的主键ID。例如：如果 source_type='shop'，则此字段存 shop_id；如果 source_type='article'，则此字段存 article_id。 */
    @Excel(name = "来源id  对应来源类型表的主键ID。例如：如果 source_type='shop'，则此字段存 shop_id；如果 source_type='article'，则此字段存 article_id。")
    private Long sourceId;

    /** 关联的1级评论id，如果是一级评论，则值为0 */
    @Excel(name = "关联的1级评论id，如果是一级评论，则值为0")
    private Long parentId;

    /** 回复的评论id */
    @Excel(name = "回复的评论id")
    private Long answerId;

    /** 评论的图片 */
    @Excel(name = "评论的图片")
    private String images;

    /** 回复的内容 */
    @Excel(name = "回复的内容")
    private String content;

    /** 点赞数 */
    @Excel(name = "点赞数")
    private Integer liked;

    @Excel(name = "回复数")
    private Integer replyCount;

    /** 状态，0：正常，1：被举报，2：禁止查看 */
    @Excel(name = "状态，0：正常，1：被举报，2：禁止查看")
    private Integer status;

    private Integer auditStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
