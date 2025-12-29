package com.smartLive.ai.entity.query;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDateTime;
@Data
public class VoucherQuery {

    /** 商铺id */
    @ToolParam(required = false, description = "商铺id")
    private Long shopId;

    /** 0,普通券；1,秒杀券 */
    private String type;

    @ToolParam(required = false, description = "商铺名称")
   private String shopName;
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
}
