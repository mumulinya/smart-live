package com.smartLive.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI Suggest Question Entity
 *
 * @author smartLive
 */
@Data
@TableName("ai_suggest_question")
public class AiSuggestQuestion implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 闂鍐呭
     */
    private String content;

    /**
     * 鍒嗙被锛?=閫氱敤 1=搴楅摵 2=鍟嗗搧 3=璇勪环
     */
    private Integer category;

    /**
     * 鎺掑簭
     */
    private Integer sort;

    /**
     * 0=绂佺敤 1=鍚敤
     */
    private Integer status;

    /**
     * 鍒涘缓鏃堕棿
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
