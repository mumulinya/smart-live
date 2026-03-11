package com.smartlive.chat.domain.VO;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.util.Map;

/**
 * 系统通知视图对象 (View Object)
 * 用于前端展现用户的系统通知、业务反馈及审核状态。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
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
