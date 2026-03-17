package com.smartLive.ai.entity.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 生成请求对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIGenerateRequest {
    private Integer sourceType;
    private List<Long> sourceIds;
}