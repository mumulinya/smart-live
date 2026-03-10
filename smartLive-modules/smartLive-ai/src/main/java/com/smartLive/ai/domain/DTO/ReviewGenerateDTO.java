package com.smartLive.ai.domain.DTO;

import lombok.Data;

/**
 * Review Generate DTO
 * 
 * @author smartLive
 */
@Data
public class ReviewGenerateDTO {
    
    /**
     * 店铺ID
     */
    private Long shopId;
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 来源类型 (1:店铺 2:团购商品)
     */
    private Integer sourceType;
    
    /**
     * 来源ID
     */
    private Long sourceId;
    
    /**
     * 总体评分 1-5
     */
    private Integer score;
    
    /**
     * 服务评分
     */
    private Short serviceScore;
    
    /**
     * 口味评分
     */
    private Short tasteScore;
    
    /**
     * 环境评分
     */
    private Short envScore;
    
    /**
     * 用户简短描述，如"底料很香，等位久"
     */
    private String description;
}
