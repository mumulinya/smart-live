package com.smartLive.ai.tools;

import com.smartLive.ai.entity.vo.ProductVO;
import com.smartLive.ai.service.rag.IProductRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * 商品管理相关 AI 工具集
 * 供 Spring AI Agent 自动调度，支持商品搜索、代金券查询及在线下单功能
 *
 * @author smartLive
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ProductTools {

    @Autowired
    private IProductRagService productRagService;

    @Tool(name = "listProduct", description = "查询商品或优惠券列表。适用场景：1.直接问商品'有什么好吃的套餐'；2.问某店铺'星巴克有什么优惠'；3.按类型找'有什么代金券'")
    public List<ProductVO> listProduct(
        @ToolParam(description = "用户原始问题，原样传入不要修改", required = false)
        String userMessage,

        @ToolParam(description = "店铺ID，已知具体店铺ID时才传，直接问商品不涉及具体店铺传null", required = false)
        String shopId,

        @ToolParam(description = "商品类型：1=代金券（满减/抵扣），2=团购套餐（多人套餐/单人餐），不确定传null", required = false)
        Integer category,

        @ToolParam(description = "优惠券类型：0=普通券，1=秒杀券（限时抢购），用户没有特别说明传null", required = false)
        Integer type
    ){
        ProductVO product = new ProductVO();
        product.setShopId(shopId);
        product.setActivityType(type); // Assuming type maps to activityType
        product.setCategory(category);
        log.info("🔍 搜索商品 | userMessage={},shopId={}, category={}, type={}",
                userMessage, shopId, category, type);
        List<ProductVO> productList = productRagService.getProductList(product, userMessage);
        productList.forEach(productVO -> log.info("🔍 返回搜索结果：{}", productVO));
        return productList;
    }

    /**
     * 订单创建接口
     * 实现在聊天界面中直接购买或抢购指定的商品
     */
    @Tool(name = "orderProduct",description = "下单商品/代金券，示例：我要下单xxx店铺的xxx")
    public String orderProduct(
            @ToolParam(description = "商品ID，从推荐列表中获取") Long productId,
            @ToolParam(description = "店铺ID") String shopId,
            @ToolParam(description = "用户ID") Long userId
    ){
        log.info("🔍 执行商品下单工具 | shopId={}, productId={}, userId={}",
                shopId, productId, userId);
        ProductVO query = new ProductVO();
        query.setId(productId);
        query.setShopId(shopId);
        query.setUserId(userId);
        return   productRagService.orderProduct(query);
    }
}
