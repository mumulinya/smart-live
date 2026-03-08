package com.smartLive.ai.entity.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class AIChatRequest {

    @NotBlank(message = "消息内容不能为空")
    private String message; // 用户消息

    private String sessionId; // 会话 ID
    private String userId; // 用户 ID
    private Map<String, Object> context; // 扩展上下文

    private Integer pageSize = 10; // 分页大小
    private Integer pageNum = 1; // 分页页码

    private String district; // 地区
    private Double x; // 经度
    private Double y; // 纬度

    private Boolean autonomous = false; // 是否启用强自治模式
}