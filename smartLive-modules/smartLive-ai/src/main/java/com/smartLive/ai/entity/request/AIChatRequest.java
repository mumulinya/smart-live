package com.smartLive.ai.entity.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import java.util.Map;

@Data
public class AIChatRequest {
    @NotBlank(message = "消息内容不能为空")
    private String message;          // 用户消息
    
    private String sessionId;        // 会话ID
    private String userId;           // 用户ID
    private Map<String, Object> context; // 扩展上下文
    
    // 分页参数
    private Integer pageSize = 10;
    private Integer pageNum = 1;
    
    // 位置信息
    private String district;
    private Double x;
    private Double y;
}