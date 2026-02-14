package com.smartLive.ai.tools;

import com.smartLive.ai.entity.query.ShopQuery;
import com.smartLive.ai.entity.vo.ShopVO;
import com.smartLive.ai.service.rag.IShopRagService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShopTools {

    private final IShopRagService shopRagService;

    @Tool(description = "根据店铺类型搜索店铺，支持位置和价格条件。")
    public List<ShopVO> searchShopsByCategory(
            @Parameter(description = "店铺类型ID，如：美食=1", required = true)
            Long typeId,
            @Parameter(description = "商圈/区域", required = false)
            String area,
            @Parameter(description = "用户提及的地址", required = false)
            String address,
            @Parameter(description = "用户所在地区", required = false)
            String district,
            @Parameter(description = "用户经度", required = false)
            Double x,
            @Parameter(description = "用户纬度", required = false)
            Double y,
            @Parameter(description = "用户期望人均", required = false)
            Integer avgPrice,
            @Parameter(description = "排序字段", required = false)
            String field,
            @ToolParam(description = "用户原始问题", required = false)
            String userMessage
    ) throws ExecutionException, InterruptedException, TimeoutException {
        log.info("Calling searchShopsByCategory | category={}, area={}, avgPrice={}, field={}, userMessage={}, district={}, x={}, y={}",
                typeId, area, avgPrice, field, userMessage, district, x, y);

        ShopVO shopQuery = new ShopVO();
        shopQuery.setTypeId(typeId);
        shopQuery.setArea(area);
        shopQuery.setAddress(address);
        shopQuery.setAvgPrice(avgPrice);
        shopQuery.setDistrict(district);
        shopQuery.setX(x);
        shopQuery.setY(y);

        ShopQuery.Sort sort = new ShopQuery.Sort();
        sort.setField(field);
        sort.setAsc(true);

        return this.shopRagService.getShopList(shopQuery, userMessage);
    }

    @Tool(name = "getShopDetails", description = "获取单个店铺详情，包含营业时间、评分、地址等。")
    public ShopVO getShopDetails(
            @ToolParam(description = "店铺id", required = false)
            Long id,
            @ToolParam(description = "店铺名称", required = false)
            String name,
            @ToolParam(description = "是否包含评论", required = false)
            Boolean includeReviews,
            @ToolParam(description = "用户所在地区", required = false)
            String district,
            @ToolParam(description = "用户经度", required = false)
            Double x,
            @ToolParam(description = "用户纬度", required = false)
            Double y,
            @ToolParam(description = "用户原始问题", required = false)
            String userMessage
    ) throws ExecutionException, InterruptedException, TimeoutException {
        boolean incRev = Boolean.TRUE.equals(includeReviews);
        log.info("Calling getShopDetails | id={}, name={}, includeReviews={}, district={}, x={}, y={}",
                id, name, incRev, district, x, y);

        ShopVO shopVO = new ShopVO();
        shopVO.setId(id);
        shopVO.setName(name);
        shopVO.setDistrict(district);
        shopVO.setX(x);
        shopVO.setY(y);

        return this.shopRagService.getShopDetails(shopVO, userMessage);
    }

    @Tool(name = "searchGroupBuyingDeals", description = "搜索团购套餐。")
    public void searchGroupBuyingDeals(
            @ToolParam(description = "类别", required = true)
            String category,
            @ToolParam(description = "地区或商圈", required = false)
            String location,
            @ToolParam(description = "最高价格", required = false)
            Double maxPrice,
            @ToolParam(description = "最低折扣率", required = false)
            Double minDiscountRate
    ) {
        log.info("searchGroupBuyingDeals | category={}, location={}, maxPrice={}, minDiscountRate={}",
                category, location, maxPrice, minDiscountRate);
    }

    @Tool(name = "searchTantanNotes", description = "搜索探店笔记。")
    public void searchTantanNotes(
            @ToolParam(description = "关键词", required = false)
            String keyword,
            @ToolParam(description = "店铺类型", required = false)
            String category,
            @ToolParam(description = "作者类型", required = false)
            String authorType,
            @ToolParam(description = "排序方式", required = false)
            String sortBy
    ) {
        log.info("searchTantanNotes | keyword={}, category={}, authorType={}, sortBy={}",
                keyword, category, authorType, sortBy);
    }
}
