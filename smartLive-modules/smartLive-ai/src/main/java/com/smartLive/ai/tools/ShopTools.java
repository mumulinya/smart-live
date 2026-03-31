package com.smartLive.ai.tools;

import com.smartLive.ai.entity.vo.ShopVO;
import com.smartLive.ai.service.rag.IBlogRagService;
import com.smartLive.ai.service.rag.IReviewRagService;
import com.smartLive.ai.service.rag.IShopRagService;
import com.smartLive.ai.service.user.support.StructuredToolCaptureRegistry;
import com.smartLive.common.core.constant.ResourceTypeConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * 店铺工具集。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShopTools {

    private final IShopRagService shopRagService;
    private final IReviewRagService reviewRagService;
    private final IBlogRagService blogRagService;
    private final StructuredToolCaptureRegistry structuredToolCaptureRegistry;

    @Tool(description = "Search shops by category, user intent and optional location filters.")
    public List<ShopVO> searchShopsByCategory(
            @ToolParam(description = "Original user message.", required = false)
            String userMessage,
            @ToolParam(description = "Shop category id. Use this mapping exactly: 1=美食, 2=KTV, 3=丽人·美发, 4=健身运动, 5=按摩足疗, 6=美容SPA, 7=亲子游乐, 8=酒吧, 9=轰趴馆, 10=美睫·美甲.", required = true)
            Long typeId,
            @ToolParam(description = "Area name.", required = false)
            String area,
            @ToolParam(description = "Address keyword.", required = false)
            String address,
            @ToolParam(description = "District name.", required = false)
            String district,
            @ToolParam(description = "Longitude.", required = false)
            Double x,
            @ToolParam(description = "Latitude.", required = false)
            Double y,
            ToolContext toolContext
    ) {
        log.info("Calling searchShopsByCategory | category={}, area={}, userMessage={}, district={}, x={}, y={}",
                typeId, area, userMessage, district, x, y);

        ShopVO shopQuery = new ShopVO();
        shopQuery.setTypeId(typeId);
        shopQuery.setArea(area);
        shopQuery.setAddress(address);
        shopQuery.setDistrict(district);
        shopQuery.setX(x);
        shopQuery.setY(y);
        List<ShopVO> results = shopRagService.getShopList(shopQuery, userMessage);
        structuredToolCaptureRegistry.recordShopResults(toolContext, results);
        return results;
    }

    @Tool(name = "getShopDetails", description = "Get detailed information for a shop by id or name.")
    public ShopVO getShopDetails(
            @ToolParam(description = "Shop id.", required = false)
            Long id,
            @ToolParam(description = "Shop name.", required = false)
            String name,
            @ToolParam(description = "Whether to include review context.", required = false)
            Boolean includeReviews,
            @ToolParam(description = "District name.", required = false)
            String district,
            @ToolParam(description = "Longitude.", required = false)
            Double x,
            @ToolParam(description = "Latitude.", required = false)
            Double y,
            @ToolParam(description = "Original user message.", required = false)
            String userMessage,
            ToolContext toolContext
    ) throws ExecutionException, InterruptedException, TimeoutException {
        log.info("Calling getShopDetails | id={}, name={}, x={}, y={}", id, name, x, y);

        ShopVO shopVO = new ShopVO();
        shopVO.setId(id);
        shopVO.setName(name);
        shopVO.setDistrict(district);
        shopVO.setX(x);
        shopVO.setY(y);
        ShopVO result = shopRagService.getShopDetails(shopVO, userMessage);
        structuredToolCaptureRegistry.recordShopResults(toolContext, result == null ? List.of() : List.of(result));
        return result;
    }

    @Tool(name = "getShopInsight", description = "Get a combined shop insight with shop details, review summary and blog summary.")
    public String getShopInsight(
            @ToolParam(description = "Shop id. Preferred when available.", required = false)
            Long id,
            @ToolParam(description = "Shop name.", required = false)
            String name,
            @ToolParam(description = "District name.", required = false)
            String district,
            @ToolParam(description = "Longitude.", required = false)
            Double x,
            @ToolParam(description = "Latitude.", required = false)
            Double y,
            @ToolParam(description = "Original user message.", required = false)
            String userMessage,
            ToolContext toolContext
    ) {
        log.info("Calling getShopInsight | id={}, name={}", id, name);

        String safeUserMessage = (userMessage == null || userMessage.isBlank())
                ? (name == null || name.isBlank() ? "shop insight" : name)
                : userMessage;

        ShopVO query = new ShopVO();
        query.setId(id);
        query.setName(name);
        query.setDistrict(district);
        query.setX(x);
        query.setY(y);

        ShopVO shop = shopRagService.getShopDetails(query, safeUserMessage);
        structuredToolCaptureRegistry.recordShopResults(toolContext, shop == null ? List.of() : List.of(shop));
        if (shop == null) {
            return "Shop not found.";
        }

        String reviewSummary = reviewRagService.getReviewSummary(
                ResourceTypeConstants.SHOP_CODE,
                shop.getId(),
                null,
                null,
                safeUserMessage
        );
        String blogSummary = blogRagService.getShopBlogSummary(shop.getId(), safeUserMessage, 5);

        StringBuilder context = new StringBuilder("Shop insight\n");
        appendLine(context, "Shop", shop.getName());
        if (shop.getScore() > 0) {
            appendLine(context, "Score", String.valueOf(shop.getScore()));
        }
        if (shop.getAvgPrice() != null) {
            appendLine(context, "Average price", String.valueOf(shop.getAvgPrice()));
        }
        appendLine(context, "Address", shop.getAddress());
        appendLine(context, "Open hours", shop.getOpenHours());
        appendLine(context, "Distance", shop.getDistanceText());
        context.append("Review summary: ").append(reviewSummary).append('\n');
        context.append("Blog summary: ").append(blogSummary);
        return context.toString();
    }

    /**
     * 追加一行上下文内容。
     */
    private void appendLine(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append(label).append(": ").append(value).append('\n');
    }
}
