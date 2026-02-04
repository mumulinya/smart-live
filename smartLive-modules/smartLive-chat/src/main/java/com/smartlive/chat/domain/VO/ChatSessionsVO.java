package com.smartlive.chat.domain.VO;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.io.Serializable;

/**
 * 私聊会话返回对象 (View Object)
 * 用于前端展示私聊会话详情
 * 
 * @author 木木林
 * @date 2025-10-05
 */
@Data
public class ChatSessionsVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 会话ID（雪花算法） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 用户1ID（较小ID） */
    private Long maxUserId;

    /** 用户2ID（较大ID） */
    private Long lowUserId;

    private String contactName;
    private String contactAvatar;
    private Long fromUid;
    private Long toUid;
}