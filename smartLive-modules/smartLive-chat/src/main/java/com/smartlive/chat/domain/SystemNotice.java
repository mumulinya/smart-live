package com.smartlive.chat.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 系统通知实体类
 * 对应数据库表 chat_system_notice，存储系统向用户推送的各类业务通知（如点赞、评论提醒、审核反馈等）。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@Data
@TableName(value = "chat_system_notice",autoResultMap = true)
public class SystemNotice implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String noticeId;

    private Long userId;

    private Integer sourceType;

    private Long sourceId;

    private String action;

    private String title;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extraData;
    private String content;

    private String rejectReason;

    private String payload;

    private Integer readStatus;

    private Date readAt;

    private Date createTime;

    private Date updateTime;
}
