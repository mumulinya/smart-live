package com.smartLive.points.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分流水记录
 *
 * @author smartLive
 */
@Data
@TableName("points_record")
public class PointsRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 类型 1:收入 2:支出 */
    private Integer type;

    /** 变动金额（绝对值） */
    private Integer amount;

    /** 业务类型 */
    private Integer bizType;

    /** 关联业务ID */
    private String bizId;

    /** 展示文案 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 用户名称 */
    @TableField(exist = false)
    private String userName;

    /** 用户昵称 */
    @TableField(exist = false)
    private String nickName;

    /** 用户头像 */
    @TableField(exist = false)
    private String avatar;
}
