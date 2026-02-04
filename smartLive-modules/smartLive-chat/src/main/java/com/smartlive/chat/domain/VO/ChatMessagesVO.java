package com.smartlive.chat.domain.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户聊天消息返回对象 (View Object)
 * 用于前端展示聊天消息详情
 * 
 * @author 木木林
 * @date 2025-10-05
 */
@Data
public class ChatMessagesVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** id */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 会话Id */
    private Long sessionId;

    /** 发送者ID */
    private Long fromUid;

    /** 接收者ID */
    private Long toUid;

    /** 消息文本内容 */
    private String content;

    /** 1已发送 2已送达 3已读 */
    private Long status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    
    /**
     * 目标日期
     */
    private String targetDate;
    
    /**
     * 消息方向
     */
    private String direction;
    
    /**
     * 锚点ID
     */
    private Long anchorId;
}