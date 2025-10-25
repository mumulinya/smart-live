package com.smartLive.search.domain.req;

import lombok.Data;

import java.util.Map;

@Data
public class FilterSearchRequest {
    /**
     * 搜索关键字
     */
    private String keyword;

    /**
     * 过滤条件
     */
    private Map<String, Object> filters;

    /**
     * 当前页码
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer size = 10;

    private Double lat;
    private Double lon;
    private String distance="5km";
}