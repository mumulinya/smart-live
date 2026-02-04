package com.smartlive.chat.domain.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户会话列表返回对象 (View Object)
 * 用于前端展示用户会话列表详情
 * 
 * @author 木木林
 * @date 2025-10-05
 */
@Data
public class UserSessionsVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** id */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 用户id */
    private Long userId;

    /** 会话id */
    private Long sessionId;

    /** 目标用户id */
    private Long targetUid;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    
    /** 头像 */
    private String avatar;
    
    /** 未读消息数 */
    private Integer unread;
    
    /** 昵称 */
    private String nickname;
    
    /** 最后一条消息 */
    private String lastMessage;
    
    /** 最后一条消息时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastTime;
    
    /** 置顶 */
    private Boolean pin;
    
    /** 背景图 */
    private String backgroundImage;
}