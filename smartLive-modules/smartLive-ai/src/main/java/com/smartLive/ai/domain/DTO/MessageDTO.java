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

    /** 消息编号。 */
    private Long id;

    /** 会话编号。 */
    private Long sessionId;

    /** 角色（用户/助手/系统）。 */
    private String role;

    /** 消息内容。 */
    private String message;

    /** 用户编号。 */
    private Long userId;
    /** 用户区域信息 */
    private String region;
    /** 经度 */
    private Double x;
    /** 纬度 */
    private Double y;
    /** 是否启用上下文 */
    private Boolean contextMode;
    /** 消息类型（文本/图片/工具）。 */
    private String type;
    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
