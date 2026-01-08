package com.smartLive.ai.entity.request;

import lombok.Data;

@Data

public class MilvusInsertRequest {
    private String indexName;
    private Long  id;
    private Object data;
    private Integer type; // shop, blog, user, voucher
}
