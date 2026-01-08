package com.smartLive.common.rabbitmq.domain;

import lombok.Data;

import java.util.List;

@Data
public class SearchIndexBatchMessage {
    private String indexName;
    private List<?> data;
    private Integer type; // shop, blog, user, voucher
}