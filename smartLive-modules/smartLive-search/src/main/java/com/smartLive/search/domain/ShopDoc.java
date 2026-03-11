package com.smartLive.search.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 店铺索引文档对象
 * 对应 ES 中的 shops 索引。专门为地理位置搜索（LBS）优化，包含 location 坐标字段。
 * 
 * @author smartLive
 * @date 2026-03-11
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShopDoc extends BaseEntity implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 店铺唯一 ID */
    private Long id;

    /** 店铺完整名称 (支持全文检索) */
    private String name;

    /** 经营分类 ID (如：餐饮、足疗、电影) */
    private Long typeId;

    /** 店铺宣传图片列表 (逗号分隔) */
    private String images;

    /** 店铺品牌 Logo */
    private String shopLogo;

    /** 所属商圈名称 (如：西湖景区、陆家嘴) */
    private String area;

    /** 详细经营地址 */
    private String address;

    /** 坐标经度 (X 轴) */
    private Double x;

    /** 坐标纬度 (Y 轴) */
    private Double y;

    /** 人均消费参考价 */
    private Integer avgPrice;

    /** 累计总销量 */
    private Integer sold;

    /** 累计评论数 */
    private Integer comments;

    /** 综合评分 (10~50 分，显示时需除以 10) */
    private Integer score;

    /** 营业时间段描述 */
    private String openHours;
    
    /** 动态计算出的距离 (米)，仅在搜索结果返回时填充 */
    private Double distance;

    /** 
     * ES 专用地理坐标字段
     * 格式："lat,lon"，通过 updateLocation 方法根据 x,y 自动计算。
     */
    private String location;
    
    private String actionType;
    private Integer sourceType;
    private Long sourceId;

    /**
     * 更新 ES 地理坐标字符串
     * 确保 x,y 坐标赋值后自动维持 location 的一致性。
     */
    private void updateLocation() {
        if (this.x != null && this.y != null) {
            this.location = this.y + "," + this.x; // geo_point 格式：lat,lon
        }
    }

    public void setX(Double x) {
        this.x = x;
        updateLocation();
    }

    public void setY(Double y) {
        this.y = y;
        updateLocation();
    }
}

