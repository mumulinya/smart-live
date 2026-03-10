package com.smartLive.ai.domain.DTO;

import lombok.Data;

/**
 * Blog Generate DTO
 * 
 * @author smartLive
 */
@Data
public class BlogGenerateDTO {
    
    /**
     * 店铺ID，必传
     */
    private Long shopId;
    
    /**
     * 用户描述，选填，如"重点写底料和服务"
     */
    private String description;
    
    /**
     * 博客风格：0=探店笔记，1=种草推荐，2=避雷测评
     */
    private Integer style;
}
