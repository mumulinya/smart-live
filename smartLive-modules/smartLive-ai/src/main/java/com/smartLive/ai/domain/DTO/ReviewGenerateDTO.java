package com.smartLive.ai.domain.DTO;

import lombok.Data;

/**
 * Review Generate DTO
 *
 * @author smartLive
 */
@Data
public class ReviewGenerateDTO {

    /** Shop id. */
    private Long shopId;

    /** Order id. */
    private Long orderId;

    /** Source type: 2=shop, 3=blog, 4=product. */
    private Integer sourceType;

    /** Source id. */
    private Long sourceId;

    /** Overall score, range 1-5. */
    private Integer score;

    /** Service score. */
    private Short serviceScore;

    /** Taste score. */
    private Short tasteScore;

    /** Environment score. */
    private Short envScore;

    /** Additional user description. */
    private String description;
}
