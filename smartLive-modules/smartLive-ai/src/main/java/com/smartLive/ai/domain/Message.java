package com.smartLive.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
@TableName("message")
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Message ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Session ID */
    private Long sessionId;

    /** Role (user/assistant/system) */
    private String role;

    /** Message Content */
    private String content;

    /** Message Type (text/image/tool) */
    private String type;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
