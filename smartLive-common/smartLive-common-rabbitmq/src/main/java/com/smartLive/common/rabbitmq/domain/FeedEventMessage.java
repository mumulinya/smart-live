package com.smartLive.common.rabbitmq.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FeedEventMessage {
    // 1. 谁发的？(Sender)
    private Integer sourceType;  // 发送者类型 (FollowTypeEnum: USER/SHOP)
    private Long sourceId;       // 发送者ID (用户ID 或 店铺ID)

    // 2. 发了什么？(Content)
    private Long bizId;        // 业务ID (博客ID / 代金券ID / 商品ID)
    private Integer bizType;   // 业务类型 (BizTypeEnum: BLOG/COUPON/GOODS)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishTime;  // 发布时间
}