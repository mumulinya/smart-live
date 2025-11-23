package com.smartLive.index.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 最新店铺 VO（匹配前端 recentShops 单个对象）
 */
@Data
public class RecentShopVO {
    private Long id;            // 店铺ID
    private String name;        // 店铺名称（前端字段是name，不是shopName）
    private Long category;    // 分类
    private String address;     // 地址
    private Integer status;     // 状态：1营业中/0已关闭
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;  // 创建时间（格式：YYYY-MM-DD HH:mm:ss）
}