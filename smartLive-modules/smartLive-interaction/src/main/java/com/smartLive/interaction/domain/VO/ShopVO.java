package com.smartLive.interaction.domain.VO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @Description: 店铺VO
 * @Author:  mumulin
 * @Date:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShopVO {
    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商铺名称 */
    private String name;

    /** 商铺类型的id */
    private Long typeId;

    /** 商铺图片，多个图片以','隔开 */
    private String images;

    /** 店铺头像 */
    private String shopLogo;

    /** 商圈，例如陆家嘴 */
    private String area;

    /** 地址 */
    private String address;

    /** 经度 */
    private Double x;

    /** 维度 */
    private Double y;

    /** 均价，取整数 */
    private String avgPrice;

    /** 销量 */
    private Integer sold;

    /** 评论数量 */
    private Integer comments;

    /** 评分，1~5分，乘10保存，避免小数 */
    private Integer score;

    /** 营业时间，例如 10:00-22:00 */
    private String openHours;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private Double distance;
    //查找关键字
    private String keyword;

    private Boolean isFollow;

    /** 
     * 收藏数
     * 用于热度计算，代表用户的静态认可度。
     */
    private Integer stars;
    /** 
     * 关注数（粉丝数）
     * 反映店铺的长期品牌力。
     */
    private Integer fans;
}
