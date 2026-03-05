package com.smartLive.ai.entity.DOC;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)  // 忽略未知字段
public class ProductDoc implements Serializable {
    private Long id;
    private Long shopId;
    private Long typeId;
    private String shopName;
    private String name; 
    private String subTitle;
    private String rulesJson;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer activityType;
    private Integer status;
    private Integer stock;
    /** 有效期类型：1-固定日期，2-动态有效期 */
    private Integer validityType;

    /** 固定日期的开始/结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date useStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date useEndTime;

    /** 动态有效期：领取后多少天有效 */
    private Integer validDays;

    /** 封面图片 */
    private String coverImg;

    /** 秒杀开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date beginTime;

    /** 秒杀结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
}
