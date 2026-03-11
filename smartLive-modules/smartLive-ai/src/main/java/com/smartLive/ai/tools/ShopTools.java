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

/**
 * 店铺查询相关 AI 工具集
 * 供 AI Agent 调用，支持按类目、地理位置及详细信息的检索
 *
 * @author smartLive
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShopTools {

    private final IShopRagService shopRagService;

    /**
     * 根据店铺类型搜索店铺，支持位置和价格条件。
     *
     * @param userMessage 用户原始问题，原样传入不要修改
     * @param typeId 店铺类型ID，必须按语义映射：美食=1，KTV=2，丽人=3，运动健身=5，酒吧=8
     * @param area 商圈或区域关键词，如：万达、天河城、北京路，用户没提就传null
     * @param address 用户提及的具体街道或地址，如：中山大道88号，用户没提就传null
     * @param district 用户所在行政区，如：天河区、南海区、三水区，从用户问题中提取，提取不到就传null
     * @param x 用户当前位置的经度，必须是Double数字如113.0528，绝对禁止传文字，获取不到传null
     * @param y 用户当前位置的纬度，必须是Double数字如23.1428，绝对禁止传文字，获取不到传null
     * @return 符合条件的店铺列表
     */
    @Tool(description = "根据店铺类型搜索店铺，支持位置和价格条件。")
    public List<ShopVO> searchShopsByCategory(
        @ToolParam(description = "用户原始问题，原样传入不要修改", required = false)
        String userMessage,

        @ToolParam(description = "店铺类型ID，必须按语义映射：美食=1，KTV=2，丽人=3，运动健身=5，酒吧=8", required = true)
        Long typeId,

        @ToolParam(description = "商圈或区域关键词，如：万达、天河城、北京路，用户没提就传null", required = false)
        String area,

        @ToolParam(description = "用户提及的具体街道或地址，如：中山大道88号，用户没提就传null", required = false)
        String address,

        @ToolParam(description = "用户所在行政区，如：天河区、南海区、三水区，从用户问题中提取，提取不到就传null", required = false)
        String district,

        @ToolParam(description = "用户当前位置的经度，必须是Double数字如113.0528，绝对禁止传文字，获取不到传null", required = false)
        Double x,

        @ToolParam(description = "用户当前位置的纬度，必须是Double数字如23.1428，绝对禁止传文字，获取不到传null", required = false)
        Double y
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

        ShopQuery.Sort sort = new ShopQuery.Sort();
        sort.setAsc(true);
        return this.shopRagService.getShopList(shopQuery, userMessage);
    }

    /**
     * 获取单个店铺的深度详情
     * 用于在用户明确指定店铺名称或从搜索列表中二次确认时调用
     */
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
        log.info("🔍 执行获取店铺详情工具 | id={}, name={}, x={}, y={}",
                id, name, x, y);

        ShopVO shopVO = new ShopVO();
        shopVO.setId(id);
        shopVO.setName(name);
        shopVO.setDistrict(district);
        shopVO.setX(x);
        shopVO.setY(y);

        return this.shopRagService.getShopDetails(shopVO, userMessage);
    }
}
