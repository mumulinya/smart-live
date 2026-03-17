package com.smartLive.ai.entity.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * AI 聊天请求对象。
 * 封装前端发起的对话请求参数，包括用户信息、位置信息及模式设置。
 */
@Data
public class AIChatRequest {

    /** 用户输入的原始消息内容 */
    @NotBlank(message = "消息内容不能为空")
    private String message;

    /** 当前会话编号（历史会话续聊时必传） */
    private String sessionId;

    /** 用户编号 */
    private String userId;

    /** 灵活扩展的上下文参数 */
    private Map<String, Object> context;

    /** 分页大小（用于召回检索增强生成数据或历史记录） */
    private Integer pageSize = 10;

    /** 当前页码 */
    private Integer pageNum = 1;

    /** 用户所在行政区（如：天河区） */
    private String district;

    /** 用户当前经度 */
    private Double x;

    /** 用户当前纬度 */
    private Double y;

    /** 是否启用强自主反思模式 */
    private Boolean autonomous = false;
}
