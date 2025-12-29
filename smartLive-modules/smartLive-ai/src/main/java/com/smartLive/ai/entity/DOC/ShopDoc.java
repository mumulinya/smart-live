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
 * @author mumulin
 * @date 2025-09-21
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

    // 添加location字段
    private String location;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    private void updateLocation() {
        if (this.x != null && this.y != null) {
            this.location = this.y + "," + this.x; // geo_point格式：lat,lon
        }
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTypeId() {
        return typeId;
    }

    public void setTypeId(Long typeId) {
        this.typeId = typeId;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
        updateLocation();
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
        updateLocation();
    }
    public Integer getSold() {
        return sold;
    }

    public void setSold(Integer sold) {
        this.sold = sold;
    }

    public Integer getComments() {
        return comments;
    }

    public void setComments(Integer comments) {
        this.comments = comments;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getOpenHours() {
        return openHours;
    }

    public void setOpenHours(String openHours) {
        this.openHours = openHours;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

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
