package com.smartLive.common.rabbitmq.domain;

import lombok.Data;

@Data

public class ContentSyncMessage {
    private String indexName;
    private Long  id;
    private Object data;
    private Integer type;
}
