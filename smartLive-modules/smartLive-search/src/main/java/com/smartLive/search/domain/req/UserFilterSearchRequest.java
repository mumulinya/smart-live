package com.smartLive.search.domain.req;

import lombok.Data;

import java.util.Map;

@Data
public class UserFilterSearchRequest {
    /**
     * 搜索关键字
     */
    private String keyword;

    private Long userId;
    /**
     * 数据类型
     */
    private Integer sourceType;
    /**
     * 动作类型
     */
    private String actionType;

    /**
     * 当前页码
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer size = 10;
}