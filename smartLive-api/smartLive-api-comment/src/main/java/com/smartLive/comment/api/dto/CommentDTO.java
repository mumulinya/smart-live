package com.smartLive.comment.api.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


/**
 * 评论对象 tb_comments
 *
 * @author mumulin
 * @date 2025-10-02
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentDTO
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户id */
    private Long userId;

    /** 来源类型  1（店铺）, 2（文章）, 3（团购）等。 */
    private Integer sourceType;

    /** 来源id  对应来源类型表的主键ID。例如：如果 source_type='shop'，则此字段存 shop_id；如果 source_type='article'，则此字段存 article_id。 */
    private Long sourceId;

    /** 来源名称 */
    private String sourceName;

    /** 关联的1级评论id，如果是一级评论，则值为0 */
    private Long parentId;

    /** 回复的评论id */
    private Long answerId;

    /** 评论的图片 */
    private String images;

    /** 回复的内容 */
    private String content;

    /** 点赞数 */
    private Integer liked;

    /** 状态，0：正常，1：被举报，2：禁止查看 */
    private Integer status;

    /** 评分 */
    private Integer rating;
    /** 用户昵称 */
    private String nickName;
    /** 用户头像 */
    private String userIcon;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 是否是AI生成的评论 */
    private Boolean isAIGenerated;

    @Override
    public String toString() {
        return "CommentVO{" +
                "id=" + id +
                ", userId=" + userId +
                ", sourceType=" + sourceType +
                ", sourceId=" + sourceId +
                ", sourceName='" + sourceName + '\'' +
                ", parentId=" + parentId +
                ", answerId=" + answerId +
                ", images='" + images + '\'' +
                ", content='" + content + '\'' +
                ", liked=" + liked +
                ", status='" + status + '\'' +
                ", rating=" + rating +
                ", nickName='" + nickName + '\'' +
                ", userIcon='" + userIcon + '\'' +
                '}';
    }
}
