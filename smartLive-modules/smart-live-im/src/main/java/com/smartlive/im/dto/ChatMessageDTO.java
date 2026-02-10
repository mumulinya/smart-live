package com.smartlive.im.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 聊天消息传输对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDTO {
    private Long id;
    private Long sessionId;
    private Long fromUid;
    private Long toUid;
    private String content;
    private Long status; // 1已发送 2已送达 3已读
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
