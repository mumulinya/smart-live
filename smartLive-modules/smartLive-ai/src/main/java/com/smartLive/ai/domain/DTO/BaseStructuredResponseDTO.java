package com.smartLive.ai.domain.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户侧结构化响应基础 DTO。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseStructuredResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private String replyText;
}
