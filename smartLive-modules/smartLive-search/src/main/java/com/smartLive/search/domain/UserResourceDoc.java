package com.smartLive.search.domain;


import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.Date;

/**
 * 聚合索引实体类
 * 包含：收藏的店铺、领取的代金券、点赞的笔记
 */
@Data
public class UserResourceDoc<T> {

    private String sourceType;
    // 原始业务ID (shopId, voucherId)
    private Long sourceId;
    // 动作区分字段： "FAVORITE", "LIKE", "HISTORY"
    private String actionType;
    //实体数据
    T data;
}