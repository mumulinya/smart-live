package com.smartLive.ai.domain.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * AI Chat Message Entity
 *
 * @author smartLive
 */
@Data
@ToString(callSuper = true)
public class MessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Message ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Session ID */
    private Long sessionId;

    /** Role (user/assistant/system) */
    private String role;

    /** Message Content */
    private String message;

    private Long userId;
    // 位置信息
    private String district;
    private Double x;
    private Double y;
    /** 是否使用上下文 */
    private Boolean contextMode;
    /** Message Type (text/image/tool) */
    private String type;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
