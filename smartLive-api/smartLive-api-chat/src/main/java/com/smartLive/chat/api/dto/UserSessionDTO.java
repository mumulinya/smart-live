package com.smartLive.chat.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 用户会话传输对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSessionDTO {
    private Long id;
    private Long userId;
    private Long sessionId;
    private Long targetUid;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
