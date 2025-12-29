package com.smartLive.ai.entity.request;

import lombok.Data;

import java.util.List;

@Data
public class MilvusBatchInsertRequest {
    private String indexName;
    private List<?> data;
    private String dataType; // shop, blog, user, voucher
}