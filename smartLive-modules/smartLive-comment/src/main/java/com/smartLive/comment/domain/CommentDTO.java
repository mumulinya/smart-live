package com.smartLive.comment.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 评论对象 tb_comments
 *
 * @author mumulin
 * @date 2025-10-02
 */
@TableName("tb_comments")
@Data
public class CommentDTO extends BaseEntity
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

    /** 来源名称 */
    private String sourceName;

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
    /** 用户昵称 */
    @TableField(exist = false)
    private String nickName;
    /** 用户头像 */
    @TableField(exist = false)
    private String userIcon;

    /** 是否是AI生成的评论 */
    private Boolean isAIGenerated;


    public String getSourceName() {
        return sourceName;
    }
    public void setSourceName(String sourceName) {}

    public String getUserIcon() {
        return userIcon;
    }

    public void setUserIcon(String userIcon) {
        this.userIcon = userIcon;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setSourceType(Integer sourceType)
    {
        this.sourceType = sourceType;
    }

    public Integer getSourceType()
    {
        return sourceType;
    }

    public void setSourceId(Long sourceId)
    {
        this.sourceId = sourceId;
    }

    public Long getSourceId()
    {
        return sourceId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setAnswerId(Long answerId)
    {
        this.answerId = answerId;
    }

    public Long getAnswerId()
    {
        return answerId;
    }

    public void setImages(String images)
    {
        this.images = images;
    }

    public String getImages()
    {
        return images;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getContent()
    {
        return content;
    }

    public void setLiked(Integer liked)
    {
        this.liked = liked;
    }

    public Integer getLiked()
    {
        return liked;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    public void setRating(Integer rating)
    {
        this.rating = rating;
    }

    public Integer getRating()
    {
        return rating;
    }

    @Override
    public String toString() {
        return "CommentDTO{" +
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
