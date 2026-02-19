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

    @Tool(name = "listProduct",description = "列出某类商铺或特定店铺的可用商品/优惠。示例：'星巴克有什么优惠？'")
    public List<ProductVO> listProduct(
            @ToolParam( description = "商户类型, 1:美食, 2:KTV, 3:丽人...") Long typeId,
            @ToolParam( description = "店铺ID") Long shopId,
            @ToolParam( description = "当前页码") Integer current,
            @ToolParam( description = "店铺名称") String shopName,
            @ToolParam( description = "优惠券类型，0普通券，1秒杀券") Integer type
    ) {
        ProductVO product = new ProductVO();
        product.setTypeId(typeId);
        product.setShopId(shopId);
        product.setShopName(shopName);
        product.setActivityType(type); // Assuming type maps to activityType
        String userMessage = "";
        List<ProductVO> productList = productRagService.getProductList(product, userMessage);
        productList.forEach(productVO -> log.info("🔍 返回搜索结果：{}", productVO));
        return productList;
    }

    @Tool(name = "orderProduct",description = "下单商品/代金券，示例：我要下单xxx店铺的xxx")
    public String orderProduct(
            @ToolParam( description = "店铺名称") String shopName,
            @ToolParam( description = "代金券名称") String voucherName,
            @ToolParam( description = "代金券类型") Integer type,
            @ToolParam( description = "用户ID") Long userId,
            @ToolParam( description = "用户消息") String userMessage
    ){
        log.info("抢购优惠券 | shopName={}, type={}, voucherName={}, userId={},userMessage={}",
                shopName, type, voucherName, userId,userMessage);
        ProductVO query = new ProductVO();
        query.setShopName(shopName);
        query.setName(voucherName); // Assuming name maps to voucherName
        query.setActivityType(type);
        query.setUserId(userId);
        return   productRagService.orderProduct(query);
    }
}
