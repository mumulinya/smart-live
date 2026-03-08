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

        @ToolParam(description = "商户类型ID，按语义映射：美食=1，KTV=2，丽人=3，运动健身=5，酒吧=8。用户直接问商品没提类型就传null", required = false)
        Long typeId,

        @ToolParam(description = "店铺ID，已知具体店铺ID时才传，直接问商品不涉及具体店铺传null", required = false)
        String shopId,

        @ToolParam(description = "店铺名称，用户明确说了店铺名才传，如：星巴克、海底捞。直接问商品不涉及店铺传null", required = false)
        String shopName,

        @ToolParam(description = "商品类型：1=代金券（满减/抵扣），2=团购套餐（多人套餐/单人餐），不确定传null", required = false)
        Integer category,

        @ToolParam(description = "优惠券类型：0=普通券，1=秒杀券（限时抢购），用户没有特别说明传null", required = false)
        Integer type
    ){
        ProductVO product = new ProductVO();
        product.setTypeId(typeId);
        product.setShopId(shopId);
        product.setShopName(shopName);
        product.setActivityType(type); // Assuming type maps to activityType
        product.setCategory(category);
        log.info("🔍 搜索商品 | userMessage={}, typeId={}, shopId={}, shopName={}, category={}, type={}",
                userMessage, typeId, shopId, shopName, category, type);
        List<ProductVO> productList = productRagService.getProductList(product, userMessage);
        productList.forEach(productVO -> log.info("🔍 返回搜索结果：{}", productVO));
        return productList;
    }

    @Tool(name = "orderProduct",description = "下单商品/代金券，示例：我要下单xxx店铺的xxx")
    public String orderProduct(
            @ToolParam( description = "店铺名称") String shopName,
            @ToolParam( description = "代金券名称") String voucherName,
            @ToolParam( description = "优惠券类型：0-普通券，1-秒杀券") Integer type,
            @ToolParam( description = "商品类型：1-代金券，2-团购套餐") Integer category,
            @ToolParam( description = "用户ID") Long userId,
            @ToolParam( description = "用户消息") String userMessage
    ){
        log.info("抢购优惠券 | shopName={}, type={}, category={}, voucherName={}, userId={},userMessage={}",
                shopName, type, category, voucherName, userId,userMessage);
        ProductVO query = new ProductVO();
        query.setShopName(shopName);
        query.setName(voucherName); // Assuming name maps to voucherName
        query.setActivityType(type);
        query.setCategory(category);
        query.setUserId(userId);
        return   productRagService.orderProduct(query);
    }
}
