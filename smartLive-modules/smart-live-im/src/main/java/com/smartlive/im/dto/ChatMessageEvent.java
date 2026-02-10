package com.smartlive.im.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 聊天消息事件
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageEvent {
    private String type;
    private Long fromUserId;
    private Long toUserId;
    private String content;
    private String tempId;
    private Long sessionId;
    private Long messageId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
