package com.smartLive.interaction.api.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)  // 忽略未知字段
public class ReviewDTO extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 用户id */
    private Long userId;
    /** 店铺id */
    private Long shopId;
    /** 订单id */
    private Long orderId;

    /** 来源类型  1（店铺）, 2（文章）, 3（团购）等。 */
    private Integer sourceType;

    private String sourceName;

    /** 来源id  对应来源类型表的主键ID。例如：如果 source_type='shop'，则此字段存 shop_id；如果 source_type='article'，则此字段存 article_id。 */
    private Long sourceId;

    /** 评价的图片 */
    private String images;

    /** 内容 */
    private String content;

    /** 点赞数 */
    private Integer liked;
    /** 回复数 */
    private Integer replyCount;
    /** 收藏数 */
    private Integer stared;

    /** 状态，0：正常，1：草稿，2：禁止查看 */
    private Integer status;

    private Integer auditStatus;

    /** 评分 */
    private Integer score;
    /** 服务评分 */
    private Short serviceScore;
    /** 口味评分 */
    private Short tasteScore;
    /** 环境评分 */
    private Short envScore;
    /** 是否匿名 */
    private Boolean isAnonymous;

    private Date createTime;
    /** 用户昵称 */
    private String nickName;
    /** 用户头像 */
    private String userIcon;
    private String shopImages;
    /**
     * 是否点赞过了
     */
    private Boolean isLike;
    /**
     * 是否收藏过了
     */
    private Boolean isStared;

    /**
     * 是否AI生成
     */
    private Boolean isAIGenerated;
    private String rejectReason;
}
