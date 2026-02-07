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

    @Tool( description = "根据商铺类型搜索店铺。支持按位置、价格、排序过滤。示例：'找附近的川菜馆'")
    public List<ShopVO> searchShopsByCategory(
            @Parameter(description = "商铺类型名称，如：美食、美发、健身", required = true)
            Long typeId,
            @Parameter(description = "所在区域或商圈，如：陆家嘴、长沙五一广场，北京天安门", required = false)
            String area,
            @Parameter(description = "用户搜索需求提到的地区，不包括用户所在地，如：上海浦东新区", required = false)
            String address,
            @ToolParam(description = "用户所在地区，用户搜索需求有提到地区的话，就为空", required = false)
            String district,
            @Parameter(description = "用户经度坐标", required = false)
            Double x,
            @Parameter(description = "用户纬度坐标", required = false)
            Double y,
            @Parameter(description = "价格区间描述，如：人均50以下、80-120", required = false)
            Integer avgPrice,
            @Parameter(description = "排序方式：score(评分高优先), distance_asc(距离近优先), sold(销量高优先)", required = false)
            String field,
            @ToolParam(description = "用户原始消息，用于RAG查询", required = false)
            String userMessage
    ) throws ExecutionException, InterruptedException, TimeoutException {
        log.info("🔍 正在调用searchShopsByCategory | category={}, area={}, avgPrice={}, field={},userMessage={},district={},x={},y={}",
                typeId, area, avgPrice, field, userMessage, district, x, y);

        // TODO: 实际调用 service 层，内部会根据 category 映射 typeId，解析 avgPrice，计算 x/y 距离等
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
        //调用rag
        return this.shopRagService.getShopList(shopQuery, userMessage);
    }

    @Tool(name = "getShopDetails", description = "获取某个店铺的详细信息（含地址、营业时间、评分、评论数量等）。示例：'海底捞几点关门？'")
    public ShopVO getShopDetails(
            @ToolParam(description = "店铺id", required = false)
            Long id,
            @ToolParam(description = "店铺名称", required = false)
            String name,
            @ToolParam(description = "是否包含用户评论列表，默认false", required = false)
            Boolean includeReviews,
            @ToolParam(description = "用户所在的地区", required = false)
            String district,
            @ToolParam(description = "用户原始消息，用于RAG查询", required = false)
            String userMessage
    ) throws ExecutionException, InterruptedException, TimeoutException {
        boolean incRev = Boolean.TRUE.equals(includeReviews);
        log.info("📖正在查询： getShopDetails | id={}, name={}, includeReviews={}", id, name, incRev);

        ShopVO shopVO = new ShopVO();
        shopVO.setId(id);
        shopVO.setName(name);
        //调用rag
       ShopVO vo = this.shopRagService.getShopDetails(shopVO, userMessage);
        // TODO: 查询详情 + 评论
        return vo;
    }
    @Tool(name = "searchGroupBuyingDeals", description = "搜索团购套餐。示例：'推荐一些便宜的自助餐团购'")
    public void searchGroupBuyingDeals(
            @ToolParam(description = "必须填写的类别，如：美食", required = true)
            String category,
            @ToolParam(description = "所在地区或商圈，如：陆家嘴", required = false)
            String location,
            @ToolParam(description = "最大单价，例如：100 表示不超过100元", required = false)
            Double maxPrice,
            @ToolParam(description = "最低折扣率，0.5表示五折及以上", required = false)
            Double minDiscountRate
    ) {
        log.info("🛒 searchGroupBuyingDeals | category={}, location={}, maxPrice={}, minDiscountRate={}",
                category, location, maxPrice, minDiscountRate);
        return ;
    }

    @Tool(name = "searchTantanNotes", description = "搜索用户发布的探店笔记（图文内容）。示例：'最近有人分享过日料探店吗？要环境好的。'")
    public void searchTantanNotes(
            @ToolParam(description = "关键词，如：火锅、拍照好看、适合约会", required = false)
            String keyword,
            @ToolParam(description = "店铺类型，如：美食", required = false)
            String category,
            @ToolParam(description = "作者类型：普通用户、达人、认证博主", required = false)
            String authorType,
            @ToolParam(description = "排序方式：latest(最新)、most_likes(点赞最多)、most_comments(评论最多)", required = false)
            String sortBy
    ) {
        log.info("📝 searchTantanNotes | keyword={}, category={}, authorType={}, sortBy={}",
                keyword, category, authorType, sortBy);
    }
}
