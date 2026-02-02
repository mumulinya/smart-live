package com.smartLive.common.rabbitmq.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResourceMessage {
    private String indexName;
    private String id;
    private Long userId;
    private Integer sourceType;
    private Long sourceId;
    private String actionType;
    private Object data;
}
