package com.smartLive.interaction.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FeedEventDTO {
    // 1. 谁发的？(Sender)
    private Long sourceId;       // 发送者ID (用户ID 或 店铺ID)
    private Integer sourceType;  // 发送者类型 (FollowTypeEnum: USER/SHOP)

    // 2. 发了什么？(Content)
    private Long bizId;        // 业务ID (博客ID / 代金券ID / 商品ID)
    private Integer bizType;   // 业务类型 (BizTypeEnum: BLOG/COUPON/GOODS)

    // 3. 额外信息 (可选)
    private String title;      // 标题 (比如 "店铺上新了50元代金券")
    private String coverImage; // 封面图 (用于消息列表展示)
    private Long publishTime;  // 发布时间
}