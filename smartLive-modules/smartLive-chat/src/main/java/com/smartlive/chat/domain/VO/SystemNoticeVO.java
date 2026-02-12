package com.smartlive.chat.domain.VO;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.util.Map;

@Data
public class SystemNoticeVO {

    private String noticeId;

    private Integer sourceType;

    private Long sourceId;

    private String action;

    private String title;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extraData;

    private String content;

    private String rejectReason;

    private String createdAt;

    private Boolean read;
}
