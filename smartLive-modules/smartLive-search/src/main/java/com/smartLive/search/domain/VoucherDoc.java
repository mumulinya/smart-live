package com.smartLive.search.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartLive.common.core.annotation.Excel;
import com.smartLive.common.core.web.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 优惠券对象 tb_voucher
 * 
 * @author ruoyi
 * @date 2025-09-21
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)  // 忽略未知字段
public class VoucherDoc extends BaseEntity implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 商铺id */
    private Long shopId;

    /** 代金券标题 */
    private String title;

    /** 副标题 */
    private String subTitle;

    /** 使用规则 */
    private String rules;

    /** 支付金额，单位是分。例如200代表2元 */
    private String payValue;

    /** 抵扣金额，单位是分。例如200代表2元 */
    private Long actualValue;

    /** 0,普通券；1,秒杀券 */
    private Integer type;

    /** 1,上架; 2,下架; 3,过期 */
    private Integer status;

    /**
     * 库存
     */
    @TableField(exist = false)
    private Integer stock;

    /**
     * 生效时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date beginTime;

    /**
     * 失效时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    //店铺名称
    private String shopName;
    //店铺类型
    private Long typeId;
}
