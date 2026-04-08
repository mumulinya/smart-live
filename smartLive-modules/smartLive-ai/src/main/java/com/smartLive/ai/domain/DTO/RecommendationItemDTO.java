package com.smartLive.ai.domain.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 推荐项 DTO。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecommendationItemDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String subTitle;
    private String type;
    private String shopId;
    private String shopName;
    private Integer category;
    private Integer activityType;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Double score;
    private String distanceText;
    private Integer avgPrice;
    private String openHours;
    private String address;
    private String area;
    private String images;
    private String shopLogo;
    private String coverImg;
    private String aiSuggestion;
    private String categoryName;
    private List<String> tags;
    private Double x;
    private Double y;
    private Integer sold;
    private Integer stock;
    private Integer validityType;
    private Date beginTime;
    private Date endTime;
    private Date useStartTime;
    private Date useEndTime;
    private Integer validDays;
}
