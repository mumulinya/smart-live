package com.smartLive.interaction.domain.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 评论视图对象 (View Object)
 * 用于前端展现评论及其关联的用户、点赞状态、资源信息。
 *
 * @author smartLive
 * @date 2026-03-11
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentVO extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 用户id */
    private Long userId;

    /** 来源类型  1（店铺）, 2（文章）, 3（团购）等。 */
    @Excel(name = "来源类型  1", readConverterExp = "店=铺")
    private Integer sourceType;

    /** 来源名称 */
    private String sourceName;

    /** 来源id */
    @Excel(name = "来源id")
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

    /** 回复数 */
    @Excel(name = "回复数")
    private Integer replyCount;

    /** 状态，0：正常，1：被举报，2：禁止查看 */
    @Excel(name = "状态，0：正常，1：被举报，2：禁止查看")
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 用户昵称 */
    private String nickName;
    /** 用户头像 */
    private String userIcon;
    /** 是否是AI生成的评论 */
    private Boolean isAIGenerated;
    /** 店铺图片 */
    private String shopImages;
    /** 是否点赞 */
    private Boolean isLike;

    /** 排序依据：如 latest 最新，hot 最热 */
    private String sort;
}
