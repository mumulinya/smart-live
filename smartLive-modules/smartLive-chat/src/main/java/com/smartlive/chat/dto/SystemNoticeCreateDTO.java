package com.smartlive.chat.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.util.Map;

@Data
public class SystemNoticeCreateDTO {

    private String noticeId;

    private Long userId;

    private Integer sourceType;

    private Long sourceId;

    private String action;

    private String title;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extraData;
    private String content;
    private String payload;
    private String rejectReason;
}
