package com.smartLive.interaction.domain.BO;

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

import java.util.Date;

/**
 * 评价对象 tb_comments
 *
 * @author mumulin
 * @date 2025-10-02
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditReviewBO extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户id */
    @Excel(name = "用户id")
    private Long userId;
    /** 店铺id */
    @Excel(name = "店铺id")
    private Long shopId;
    /** 订单id */
    @Excel(name = "订单id")
    private Long orderId;

    /** 来源类型  1（店铺）, 2（文章）, 3（团购）等。 */
    @Excel(name = "来源类型  1", readConverterExp = "店=铺")
    private Integer sourceType;

    @TableField(exist = false)
    private String sourceName;

    /** 来源id  对应来源类型表的主键ID。例如：如果 source_type='shop'，则此字段存 shop_id；如果 source_type='article'，则此字段存 article_id。 */
    @Excel(name = "来源id  对应来源类型表的主键ID。例如：如果 source_type='shop'，则此字段存 shop_id；如果 source_type='article'，则此字段存 article_id。")
    private Long sourceId;

    /** 评价的图片 */
    @Excel(name = "评论的图片")
    private String images;

    /** 内容 */
    @Excel(name = "内容")
    private String content;

    /** 点赞数 */
    @Excel(name = "点赞数")
    private Integer liked;
    /** 回复数 */
    @Excel(name = "回复数")
    private Integer replyCount;
    /** 收藏数 */
    @Excel(name = "收藏数")
    private Integer stared;

    /** 状态，0：正常，1：草稿，2：禁止查看 */
    @Excel(name = "状态，0：正常，1：被举报，2：禁止查看")
    private Integer status;

    /** 评分 */
    @Excel(name = "评分")
    private Integer score;
    /** 服务评分 */
    @Excel(name = "服务评分")
    private Short serviceScore;
    /** 口味评分 */
    @Excel(name = "口味评分")
    private Short tasteScore;
    /** 环境评分 */
    @Excel(name = "环境评分")
    private Short envScore;
    /** 是否匿名 */
    @Excel(name = "是否匿名")
    private Boolean isAnonymous;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /** 用户昵称 */
    @TableField(exist = false)
    private String nickName;
    /** 用户头像 */
    @TableField(exist = false)
    private String userIcon;
    @TableField(exist = false)
    private String shopImages;
    /** 目标标题 */
    @Excel(name = "目标标题")
    private String targetTitle;
    @Excel(name = "目标图片")
    private String targetImages;
}
