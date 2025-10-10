package com.smartLive.marketing.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券对象 tb_voucher
 * 
 * @author ruoyi
 * @date 2025-09-21
 */
@TableName("tb_voucher")
public class Voucher extends BaseEntity implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商铺id */
    @Excel(name = "商铺id")
    private Long shopId;

    /** 代金券标题 */
    @Excel(name = "代金券标题")
    private String title;

    /** 副标题 */
    @Excel(name = "副标题")
    private String subTitle;

    /** 使用规则 */
    @Excel(name = "使用规则")
    private String rules;

    /** 支付金额，单位是分。例如200代表2元 */
    @Excel(name = "支付金额，单位是分。例如200代表2元")
    private String payValue;

    /** 抵扣金额，单位是分。例如200代表2元 */
    @Excel(name = "抵扣金额，单位是分。例如200代表2元")
    private Long actualValue;

    /** 0,普通券；1,秒杀券 */
    @Excel(name = "0,普通券；1,秒杀券")
    private String type;

    /** 1,上架; 2,下架; 3,过期 */
    @Excel(name = "1,上架; 2,下架; 3,过期")
    private String status;

    /**
     * 库存
     */
    @TableField(exist = false)
    private Integer stock;

    /**
     * 生效时间
     */
    @TableField(exist = false)
    private LocalDateTime beginTime;

    /**
     * 失效时间
     */
    @TableField(exist = false)
    private LocalDateTime endTime;
    //店铺名称
    @TableField(exist = false)
    private String shopName;
    //店铺类型
    @TableField(exist = false)
    private Long typeId;

    public Long getTypeId() {
        return typeId;
    }

    public void setTypeId(Long typeId) {
        this.typeId = typeId;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public LocalDateTime getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(LocalDateTime beginTime) {
        this.beginTime = beginTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Long getId()
    {
        return id;
    }

    public void setShopId(Long shopId) 
    {
        this.shopId = shopId;
    }

    public Long getShopId() 
    {
        return shopId;
    }

    public void setTitle(String title) 
    {
        this.title = title;
    }

    public String getTitle() 
    {
        return title;
    }

    public void setSubTitle(String subTitle) 
    {
        this.subTitle = subTitle;
    }

    public String getSubTitle() 
    {
        return subTitle;
    }

    public void setRules(String rules) 
    {
        this.rules = rules;
    }

    public String getRules() 
    {
        return rules;
    }

    public void setPayValue(String payValue) 
    {
        this.payValue = payValue;
    }

    public String getPayValue() 
    {
        return payValue;
    }

    public void setActualValue(Long actualValue) 
    {
        this.actualValue = actualValue;
    }

    public Long getActualValue() 
    {
        return actualValue;
    }

    public void setType(String type) 
    {
        this.type = type;
    }

    public String getType() 
    {
        return type;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("shopId", getShopId())
            .append("title", getTitle())
            .append("subTitle", getSubTitle())
            .append("rules", getRules())
            .append("payValue", getPayValue())
            .append("actualValue", getActualValue())
            .append("type", getType())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
