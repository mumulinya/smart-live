package com.smartLive.ai.service.generate.impl;

import com.smartLive.ai.domain.DTO.ReviewGenerateDTO;
import com.smartLive.ai.entity.vo.ShopVO;
import com.smartLive.ai.service.generate.IReviewGenerateService;
import com.smartLive.ai.service.rag.IShopRagService;
import com.smartLive.ai.service.rag.IProductRagService;
import com.smartLive.ai.entity.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
public class ReviewGenerateServiceImpl implements IReviewGenerateService {

    private final ChatClient chatClient;
    private final IShopRagService shopRagService;
    private final IProductRagService productRagService;

    @Autowired
    public ReviewGenerateServiceImpl(
            @Qualifier("generalChatClient") ChatClient chatClient,
            IShopRagService shopRagService,
            IProductRagService productRagService) {
        this.chatClient = chatClient;
        this.shopRagService = shopRagService;
        this.productRagService = productRagService;
    }

    @Override
    public String generate(ReviewGenerateDTO dto) {
        String shopDetails = "未知店铺";
        String productDetails = null;
        
        // 1. 店铺信息必读取
        Long shopIdToQuery = dto.getShopId() != null ? dto.getShopId() : (dto.getSourceType() != null && dto.getSourceType() == 2 ? dto.getSourceId() : null);
        if (shopIdToQuery != null) {
            ShopVO searchVo = new ShopVO();
            searchVo.setId(shopIdToQuery);
            ShopVO shopInfo = shopRagService.getShopDetails(searchVo, "店铺详情");
            if (shopInfo != null && shopInfo.getName() != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("店名: ").append(shopInfo.getName()).append(", ");
                if (shopInfo.getArea() != null) sb.append("商圈: ").append(shopInfo.getArea()).append(", ");
                if (shopInfo.getAddress() != null) sb.append("地址: ").append(shopInfo.getAddress()).append(", ");
                if (shopInfo.getAvgPrice() != null) sb.append("人均: ").append(shopInfo.getAvgPrice()).append("元, ");
                if (shopInfo.getScore() > 0) sb.append("评分(满分5): ").append(shopInfo.getScore()).append("分, ");
                if (shopInfo.getOpenHours() != null) sb.append("营业时间: ").append(shopInfo.getOpenHours());
                shopDetails = sb.toString();
            }
        }

        // 2. 如果是商品，额外读取商品信息
        if (dto.getSourceType() != null && dto.getSourceType() == 4) {
            if (dto.getSourceId() != null) {
                ProductVO searchProduct = new ProductVO();
                searchProduct.setId(dto.getSourceId());
                List<ProductVO> products = productRagService.getProductList(searchProduct, "商品详情");
                if (products != null && !products.isEmpty() && products.get(0) != null && products.get(0).getName() != null) {
                    ProductVO p = products.get(0);
                    StringBuilder sb = new StringBuilder();
                    sb.append("商品名称: ").append(p.getName()).append(", ");
                    if (p.getSubTitle() != null) sb.append("副标题: ").append(p.getSubTitle()).append(", ");
                    if (p.getPrice() != null) sb.append("现价: ").append(p.getPrice()).append("元, ");
                    if (p.getOriginalPrice() != null) sb.append("原价: ").append(p.getOriginalPrice()).append("元, ");
                    if (p.getRulesJson() != null) sb.append("规则/内容: ").append(p.getRulesJson());
                    productDetails = sb.toString();
                }
            }
        }

        // 3. 构建提示词
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个大众点评的真实消费者。请根据以下评分和体验，写一段真实自然的评价（不要太像 AI，要接地气）。\n\n");
        
        prompt.append("【关联店铺信息】: ").append(shopDetails).append("\n");
        if (productDetails != null) {
            prompt.append("【购买商品/代金券信息】: ").append(productDetails).append("\n");
        }
        if (dto.getScore() != null) prompt.append("【综合评分】: ").append(dto.getScore()).append(" 星 (满分5星)\n");
        if (dto.getTasteScore() != null) prompt.append("【口味评分】: ").append(dto.getTasteScore()).append(" 分\n");
        if (dto.getEnvScore() != null) prompt.append("【环境评分】: ").append(dto.getEnvScore()).append(" 分\n");
        if (dto.getServiceScore() != null) prompt.append("【服务评分】: ").append(dto.getServiceScore()).append(" 分\n");
        prompt.append("\n");
        
        if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
            prompt.append("【我的主观体验描述】: ").append(dto.getDescription()).append("\n\n");
        }

        prompt.append("【生成要求】\n");
        int score = dto.getScore() != null ? dto.getScore() : 5;
        if (score == 5) {
            prompt.append("- 语气：热情洋溢，非常满意，强烈推荐。\n");
        } else if (score == 4) {
            prompt.append("- 语气：正面为主，肯定优点，也许带有一点小瑕疵或小建议。\n");
        } else if (score == 3) {
            prompt.append("- 语气：中肯客观，说明优缺点，认为总体表现平平。\n");
        } else {
            prompt.append("- 语气：失望、理性指出明显不足的地方，不带脏话但要有明确的负面反馈。\n");
        }
        
        prompt.append("- 字数控制在 50-150 字。\n");
        prompt.append("- 只输出评价正文即可，不要输出任何其他多余的解释文字。\n");

        log.info("Review Generate Prompt: {}", prompt.toString());

        // 3. 调用大模型 (带重试机制)
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                String result = chatClient.prompt().user(prompt.toString()).call().content();
                if (result != null && !result.isBlank()) {
                    return result;
                }
                log.warn("Review generation returned empty content, retrying... ({}/{})", i + 1, maxRetries);
            } catch (Exception e) {
                log.warn("Review generation failed, retrying... ({}/{})", i + 1, maxRetries, e);
                if (i == maxRetries - 1) {
                    throw new RuntimeException("AI 生成失败，请稍后重试", e);
                }
            }
        }
        throw new RuntimeException("AI 生成未能返回有效内容，请稍后重试");
    }
}
