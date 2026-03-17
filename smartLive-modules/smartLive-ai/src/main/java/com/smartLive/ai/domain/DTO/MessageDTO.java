package com.smartLive.ai.domain.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * AI 聊天消息数据传输对象。
 *
 * 作者：smartLive
 */
@Data
@ToString(callSuper = true)
public class MessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 消息 ID。 */
    private Long id;

    /** 会话 ID。 */
    private Long sessionId;

    /** 角色（user / assistant / system）。 */
    private String role;

    /** 消息内容。 */
    private String message;

    private Long userId;
    // 位置信息
    private String region;
    private Double x;
    private Double y;
    /** 是否使用上下文*/
    private Boolean contextMode;
    /** 消息类型（text / image / tool）。 */
    private String type;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
