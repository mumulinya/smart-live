package com.smartlive.chat.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;

import java.util.Date;

/**
 * 用户聊天消息对象 tb_chat_messages
 * 
 * @author 木木林
 * @date 2025-10-05
 */
@TableName("chat_messages")
public class ChatMessages extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** id */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 会话Id */
    @Excel(name = "会话Id")
    private Long sessionId;

    /** 发送者ID */
    @Excel(name = "发送者ID")
    private Long fromUid;

    /** 接收者ID */
    @Excel(name = "接收者ID")
    private Long toUid;

    /** 消息文本内容 */
    @Excel(name = "消息文本内容")
    private String content;

    /** 1已发送 2已送达 3已读 */
    @Excel(name = "1已发送 2已送达 3已读")
    private Long status;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Override
    public Date getCreateTime() {
        return createTime;
    }

    @Override
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setSessionId(Long sessionId) 
    {
        this.sessionId = sessionId;
    }

    public Long getSessionId() 
    {
        return sessionId;
    }

    public void setFromUid(Long fromUid) 
    {
        this.fromUid = fromUid;
    }

    public Long getFromUid() 
    {
        return fromUid;
    }

    public void setToUid(Long toUid) 
    {
        this.toUid = toUid;
    }

    public Long getToUid() 
    {
        return toUid;
    }

    public void setContent(String content) 
    {
        this.content = content;
    }

    public String getContent() 
    {
        return content;
    }

    public void setStatus(Long status) 
    {
        this.status = status;
    }

    public Long getStatus() 
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("sessionId", getSessionId())
            .append("fromUid", getFromUid())
            .append("toUid", getToUid())
            .append("content", getContent())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .toString();
    }
}
