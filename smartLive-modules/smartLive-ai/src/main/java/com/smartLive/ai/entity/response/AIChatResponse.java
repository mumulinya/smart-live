package com.smartLive.ai.entity.response;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIChatResponse {
    private boolean success;          // 是否成功
    private Flux<String> response;          // AI文本回复
    private String responseType;      // 响应类型: search, recommend, service, etc.
    private Object data;              // 结构化数据
    private List<?> listData;         // 列表数据
    private Map<String, Object> metadata; // 元数据
    
    // 错误信息
    private String errorCode;
    private String errorMessage;
    
    // 性能数据
    private Long processingTime;      // 处理耗时(ms)
    private String requestId;         // 请求ID
    
    public static AIChatResponse success(Flux<String> response, String responseType) {
        return AIChatResponse.builder()
                .success(true)
                .response(response)
                .responseType(responseType)
                .processingTime(System.currentTimeMillis())
                .build();
    }
    
    public static AIChatResponse error(String errorMessage) {
        return AIChatResponse.builder()
                .success(false)
                .errorMessage(errorMessage)
                .processingTime(System.currentTimeMillis())
                .build();
    }
}