package com.smartlive.chat.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;

/**
 * 私聊会话对象 tb_chat_sessions
 * 
 * @author 木木林
 * @date 2025-10-05
 */
@TableName("chat_sessions")
public class ChatSessions extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 会话ID（雪花算法） */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户1ID（较小ID） */
    @Excel(name = "用户1ID", readConverterExp = "较=小ID")
    private Long maxUserId;

    /** 用户2ID（较大ID） */
    @Excel(name = "用户2ID", readConverterExp = "较=大ID")
    private Long lowUserId;

    @TableField(exist = false)
    private String contactName;
    @TableField(exist = false)
    private String contactAvatar;
    @TableField(exist = false)
    private Long fromUid;
    @TableField(exist = false)
    private Long toUid;

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactAvatar() {
        return contactAvatar;
    }

    public void setContactAvatar(String contactAvatar) {
        this.contactAvatar = contactAvatar;
    }

    public Long getFromUid() {
        return fromUid;
    }

    public void setFromUid(Long fromUid) {
        this.fromUid = fromUid;
    }

    public Long getToUid() {
        return toUid;
    }

    public void setToUid(Long toUid) {
        this.toUid = toUid;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setMaxUserId(Long maxUserId) 
    {
        this.maxUserId = maxUserId;
    }

    public Long getMaxUserId() 
    {
        return maxUserId;
    }

    public void setLowUserId(Long lowUserId) 
    {
        this.lowUserId = lowUserId;
    }

    public Long getLowUserId() 
    {
        return lowUserId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("maxUserId", getMaxUserId())
            .append("lowUserId", getLowUserId())
            .append("createTime", getCreateTime())
            .toString();
    }
}
