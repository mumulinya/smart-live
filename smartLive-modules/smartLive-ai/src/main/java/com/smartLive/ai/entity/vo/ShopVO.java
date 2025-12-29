package com.smartLive.ai.entity.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 示例：ShopVO.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopVO {
    /** 商铺id */
    private Long id;
    /** 商铺名称 */
    private String name;

    /** 商铺类型的id */
    private Long typeId;

    /** 商铺图片，多个图片以','隔开 */
    private String images;

    /** 商圈，例如陆家嘴 */
    private String area;

    /** 地址 */
    private String address;

    /** 经度 */
    private Double x;

    /** 维度 */
    private Double y;

    /** 均价，取整数 */
    private Integer avgPrice;

    /** 销量 */
    private Integer sold;

    /** 评论数量 */
    private Integer comments;

    /** 评分，1~5分，乘10保存，避免小数 */
    private Integer score;

    /** 营业时间，例如 10:00-22:00 */
    private String openHours;

    @TableField(exist = false)
    /** 距离*/
    private Double distance;
    /** 所属区域，例如：浦东新区 */
    private String district;

    /** 距离文本，例如：500米 */
    private String distanceText;
}
