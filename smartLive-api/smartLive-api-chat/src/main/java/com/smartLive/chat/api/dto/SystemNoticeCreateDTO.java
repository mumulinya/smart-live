package com.smartLive.chat.api.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SystemNoticeCreateDTO {

    private String noticeId;

    private Long userId;

    private Integer sourceType;

    private Long sourceId;

    private String action;

    private String title;

    private String rejectReason;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extraData;
    private String content;
    private String payload;
}
