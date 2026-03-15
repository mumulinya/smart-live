package com.smartLive.ai.domain.DTO;

import lombok.Data;

/**
 * Blog Generate DTO
 *
 * @author smartLive
 */
@Data
public class BlogGenerateDTO {

    /** Shop id. */
    private Long shopId;

    /** Additional user requirement. */
    private String description;

    /** Style: 0=natural, 1=refined, 2=humorous. */
    private Integer style;
}
