package com.smartLive.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * AI Chat Message Entity
 *
 * @author smartLive
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName("ai_message")
public class Message extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** Message ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** Session ID */
    private Long sessionId;

    /** Role (user/assistant/system) */
    private String role;

    /** Message Content */
    private String content;

    /** Message Type (text/image/tool) */
    private String type;
}
