package com.smartLive.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * AI 聊天消息实体
 * 对应数据库 message 表，存储会话中的每一轮对话内容
 *
 * @author smartLive
 */
@Data
@ToString(callSuper = true)
@TableName("message")
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 消息 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属会话 ID */
    private Long sessionId;

    /** 角色 (user:用户, assistant:AI助手, system:系统说明) */
    private String role;

    /** 消息正文内容 */
    private String content;

    /** 消息类型 (text:纯文本, card:推荐卡片, image:图片) */
    private String type;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
