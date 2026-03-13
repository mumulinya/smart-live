package com.smartLive.ai.domain.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * AI Chat Message DTO
 *
 * @author smartLive
 */
@Data
@ToString(callSuper = true)
public class MessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Message ID */
    private Long id;

    /** Session ID */
    private Long sessionId;

    /** Role (user/assistant/system) */
    private String role;

    /** Message Content */
    private String message;

    private Long userId;
    // 浣嶇疆淇℃伅
    private String region;
    private Double x;
    private Double y;
    /** 鏄惁浣跨敤涓婁笅鏂?*/
    private Boolean contextMode;
    /** Message Type (text/image/tool) */
    private String type;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
