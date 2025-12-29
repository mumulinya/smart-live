package com.smartLive.ai.entity.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopQuery {
    @ToolParam(required = false, description = "商铺类型：1-美食 2-KTV 3-丽人·美发 4-健身运动 5-按摩·足疗 6-美容SPA 7-亲子游乐 8-酒吧 9-轰趴馆 10-美睫·美甲")
    private Long typeId;
    /** 商铺名称 */
    @ToolParam(required = false, description = "商铺名称")
    private String name;
    @ToolParam(required = false, description = "店铺所在的商圈，例如陆家嘴")
    private String area;
    @ToolParam(required = false, description = "店铺所在地址")
    private String address;

    @ToolParam(required = false, description = "经度")
    private Double x;
    @ToolParam(required = false, description = "维度")
    private Double y;

    @ToolParam(required = false, description = "均价，取整数")
    private Integer avgPrice;

    @ToolParam(required = false, description = "营业时间，例如 10:00-22:00")
    private String openHours;
    @ToolParam(required = false, description = "排序方式")
    private List<ShopQuery.Sort> sorts;
    @Data
    public static class Sort {
        @ToolParam(required = false, description = "排序字段: score或comments或sold或sold")
        private String field;
        @ToolParam(required = false, description = "是否是升序: true/false")
        private Boolean asc;
    }
}
