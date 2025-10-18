package com.smartlive.chat.dto;

import lombok.Data;
import java.util.Date;

@Data
public class ChatMessageEvent {
    private String type;
    private Long fromUserId;
    private Long toUserId;
    private String content;
    private String tempId;
    private Long sessionId;
    private Long messageId;
    private Date createTime;
}