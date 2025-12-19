package com.smartLive.interaction.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
/**
 * 社交信息VO
 */
public class SocialInfoVO {
    // === 1. 雷打不动的固定字段 (所有列表页都得有) ===
    private Long id;
    private Integer type;      // 1-User, 2-Shop
    private String name;       // 映射：用户名 OR 店铺名 OR 话题名
    private String avatar;     // 映射：用户头像 OR 店铺Logo OR 话题封面
    private Boolean isFollow;  // 是否关注

    // === 2. 差异化字段的“归一化”处理 ===
    // 用于展示列表里的“副标题”或“描述”
    private String description; 
    
    // === 3. (可选) 实在无法统一的，用Map兜底 ===
    private Map<String, Object> extras;
}