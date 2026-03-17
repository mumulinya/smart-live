package com.smartLive.ai.entity.DOC;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 店铺对象 tb_shop
 * 
 * 作者：mumulin
 * 创建日期：2025-09-21
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)  // 忽略未知字段
public class ShopDoc implements Serializable
{
    private static final long serialVersionUID = 1L;

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
    private Integer avgPrice;

    /** 销量 */
    private Integer sold;

    /** 评论数量 */
    private Integer comments;

    /** 评分，1~5分，乘10保存，避免小数 */
    private Integer score;

    /** 营业时间，例如 10:00-22:00 */
    private String openHours;
    /** 距离，单位米 */
    private Double distance;

    // 添加位置坐标字段
    private String location;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 更新位置坐标。
     */
    private void updateLocation() {
        if (this.x != null && this.y != null) {
            this.location = this.y + "," + this.x; // geo_point格式：lat,lon
        }
    }
    /**
     * 获取ID。
     */
    public Long getId() {
        return id;
    }
    /**
     * 设置ID。
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取名称。
     */
    public String getName() {
        return name;
    }

    /**
     * 设置名称。
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取类型 ID。
     */
    public Long getTypeId() {
        return typeId;
    }

    /**
     * 设置类型 ID。
     */
    public void setTypeId(Long typeId) {
        this.typeId = typeId;
    }

    /**
     * 获取图片列表。
     */
    public String getImages() {
        return images;
    }

    /**
     * 设置图片列表。
     */
    public void setImages(String images) {
        this.images = images;
    }

    /**
     * 获取商圈。
     */
    public String getArea() {
        return area;
    }

    /**
     * 设置商圈。
     */
    public void setArea(String area) {
        this.area = area;
    }

    /**
     * 获取地址。
     */
    public String getAddress() {
        return address;
    }

    /**
     * 设置地址。
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * 获取经度。
     */
    public Double getX() {
        return x;
    }

    /**
     * 设置经度。
     */
    public void setX(Double x) {
        this.x = x;
        updateLocation();
    }

    /**
     * 获取纬度。
     */
    public Double getY() {
        return y;
    }

    /**
     * 设置纬度。
     */
    public void setY(Double y) {
        this.y = y;
        updateLocation();
    }
    /**
     * 获取销量。
     */
    public Integer getSold() {
        return sold;
    }

    /**
     * 设置销量。
     */
    public void setSold(Integer sold) {
        this.sold = sold;
    }

    /**
     * 获取评论数。
     */
    public Integer getComments() {
        return comments;
    }

    /**
     * 设置评论数。
     */
    public void setComments(Integer comments) {
        this.comments = comments;
    }

    /**
     * 获取评分。
     */
    public Integer getScore() {
        return score;
    }

    /**
     * 设置评分。
     */
    public void setScore(Integer score) {
        this.score = score;
    }

    /**
     * 获取营业时间。
     */
    public String getOpenHours() {
        return openHours;
    }

    /**
     * 设置营业时间。
     */
    public void setOpenHours(String openHours) {
        this.openHours = openHours;
    }

    /**
     * 获取距离值。
     */
    public Double getDistance() {
        return distance;
    }

    /**
     * 设置距离值。
     */
    public void setDistance(Double distance) {
        this.distance = distance;
    }

    /**
     * 获取位置坐标。
     */
    public String getLocation() {
        return location;
    }

    /**
     * 设置位置坐标。
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * 转换为字符串结果。
     */
    @Override
    public String toString() {
        return "Shop{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", typeId='" + typeId + '\'' +
                ", images='" + images + '\'' +
                ", area='" + area + '\'' +
                ", address='" + address + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", avgPrice='" + avgPrice + '\'' +
                ", sold=" + sold +
                ", comments=" + comments +
                ", score=" + score +
                ", openHours='" + openHours + '\'' +
                ", distance=" + distance +
                '}';
    }
}
