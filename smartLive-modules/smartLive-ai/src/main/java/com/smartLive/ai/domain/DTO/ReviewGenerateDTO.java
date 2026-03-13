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
     * 搴楅摵ID
     */
    private Long shopId;
    
    /**
     * 璁㈠崟ID
     */
    private Long orderId;
    
    /**
     * 鏉ユ簮绫诲瀷 (1:搴楅摵 2:鍥㈣喘鍟嗗搧)
     */
    private Integer sourceType;
    
    /**
     * 鏉ユ簮ID
     */
    private Long sourceId;
    
    /**
     * 鎬讳綋璇勫垎 1-5
     */
    private Integer score;
    
    /**
     * 鏈嶅姟璇勫垎
     */
    private Short serviceScore;
    
    /**
     * 鍙ｅ懗璇勫垎
     */
    private Short tasteScore;
    
    /**
     * 鐜璇勫垎
     */
    private Short envScore;
    
    /**
     * 鐢ㄦ埛绠€鐭弿杩帮紝濡?搴曟枡寰堥锛岀瓑浣嶄箙"
     */
    private String description;
}
