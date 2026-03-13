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
     * 搴楅摵ID锛屽繀浼?
     */
    private Long shopId;
    
    /**
     * 鐢ㄦ埛鎻忚堪锛岄€夊～锛屽"閲嶇偣鍐欏簳鏂欏拰鏈嶅姟"
     */
    private String description;
    
    /**
     * 鍗氬椋庢牸锛?=鎺㈠簵绗旇锛?=绉嶈崏鎺ㄨ崘锛?=閬块浄娴嬭瘎
     */
    private Integer style;
}
