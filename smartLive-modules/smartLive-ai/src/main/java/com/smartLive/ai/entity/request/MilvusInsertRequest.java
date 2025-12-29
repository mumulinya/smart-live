package com.smartLive.ai.entity.request;

import lombok.Data;

@Data

public class MilvusInsertRequest {
    private String indexName;
    private Long  id;
    private Object data;
    private String dataType; // shop, blog, user, voucher
}
