package com.smartLive.common.core.domain;

import lombok.Data;

import java.util.List;

@Data
public class EsBatchInsertRequest {
    private String indexName;
    private List<?> data;
    private String dataType; // shop, blog, user, voucher
}