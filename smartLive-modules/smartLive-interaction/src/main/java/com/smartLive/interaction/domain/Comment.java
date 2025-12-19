package com.smartLive.interaction.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

/**
 * 评论对象 tb_comments
 *
 * @author mumulin
 * @date 2025-10-02
 */
@TableName("comments")
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

    @TableField(exist = false)
    private String sourceName;

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

    /** 状态，0：正常，1：被举报，2：禁止查看 */
    @Excel(name = "状态，0：正常，1：被举报，2：禁止查看")
    private String status;

    /** 评分 */
    @Excel(name = "评分")
    private Integer rating;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /** 用户昵称 */
    @TableField(exist = false)
    private String nickName;
    /** 用户头像 */
    @TableField(exist = false)
    private String userIcon;
    /** 是否是AI生成的评论 */
    @TableField(exist = false)
    private Boolean isAIGenerated;
}
