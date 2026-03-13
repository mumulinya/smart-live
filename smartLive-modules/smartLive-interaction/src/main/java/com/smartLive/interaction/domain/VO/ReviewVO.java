package com.smartLive.interaction.domain.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 评价返回对象 (View Object)
 * 用于前端展示评价详情，包含数据库字段及扩展字段
 *
 * @author mumulin
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewVO extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 用户id */
    private Long userId;

    /** 店铺id */
    private Long shopId;

    /** 订单id */
    private Long orderId;

    /** 来源类型 */
    private Integer sourceType;

    /** 来源id */
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

    /** 状态 */
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

    /** 审核拒绝原因 */
    private String rejectReason;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    // ========== 以下为扩展字段（非数据库字段） ==========

    /** 来源名称 */
    private String sourceName;

    /** 用户昵称 */
    private String nickName;

    /** 用户头像 */
    private String userIcon;

    /** 店铺头像 */
    private String shopLogo;

    /** 店铺名称 */
    private String shopName;

    /** 是否点赞过了 */
    private Boolean isLike;

    /** 是否收藏过了 */
    private Boolean isStared;

    /** 是否AI生成 */
    private Boolean isAIGenerated;

    /** 商品标题 */
    private String productName;

    /** 商品背景图 */
    private String productCoverImg;

    /** 商品价格 */
    private BigDecimal productPrice;
}
