package com.smartLive.ai.service.generate.impl;

import com.smartLive.ai.domain.DTO.ReviewGenerateDTO;
import com.smartLive.ai.entity.vo.ProductVO;
import com.smartLive.ai.entity.vo.ShopVO;
import com.smartLive.ai.service.generate.IReviewGenerateService;
import com.smartLive.ai.service.rag.IProductRagService;
import com.smartLive.ai.service.rag.IShopRagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 评价生成服务实现类。
 */
@Slf4j
@Service
public class ReviewGenerateServiceImpl implements IReviewGenerateService {

    private final ChatClient chatClient;
    private final IShopRagService shopRagService;
    private final IProductRagService productRagService;

    /**
     * 构造评价生成服务实现。
     */
    @Autowired
    public ReviewGenerateServiceImpl(
            @Qualifier("generalChatClient") ChatClient chatClient,
            IShopRagService shopRagService,
            IProductRagService productRagService) {
        this.chatClient = chatClient;
        this.shopRagService = shopRagService;
        this.productRagService = productRagService;
    }

    /**
     * 生成字符串结果。
     */
    @Override
    public String generate(ReviewGenerateDTO dto) {
        String shopDetails = "No shop information";
        String productDetails = null;

        Long shopIdToQuery = dto.getShopId() != null
                ? dto.getShopId()
                : (dto.getSourceType() != null && dto.getSourceType() == 2 ? dto.getSourceId() : null);
        if (shopIdToQuery != null) {
            ShopVO searchVo = new ShopVO();
            searchVo.setId(shopIdToQuery);
            ShopVO shopInfo = shopRagService.getShopDetails(searchVo, "shop details");
            if (shopInfo != null && shopInfo.getName() != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("Shop: ").append(shopInfo.getName());
                if (shopInfo.getArea() != null) {
                    sb.append(", Area: ").append(shopInfo.getArea());
                }
                if (shopInfo.getAddress() != null) {
                    sb.append(", Address: ").append(shopInfo.getAddress());
                }
                if (shopInfo.getAvgPrice() != null) {
                    sb.append(", Average price: ").append(shopInfo.getAvgPrice());
                }
                if (shopInfo.getScore() > 0) {
                    sb.append(", Score: ").append(shopInfo.getScore()).append("/5");
                }
                if (shopInfo.getOpenHours() != null) {
                    sb.append(", Open hours: ").append(shopInfo.getOpenHours());
                }
                shopDetails = sb.toString();
            }
        }

        if (dto.getSourceType() != null && dto.getSourceType() == 4 && dto.getSourceId() != null) {
            ProductVO searchProduct = new ProductVO();
            searchProduct.setId(dto.getSourceId());
            List<ProductVO> products = productRagService.getProductList(searchProduct, "product details");
            if (products != null && !products.isEmpty() && products.get(0) != null && products.get(0).getName() != null) {
                ProductVO product = products.get(0);
                StringBuilder sb = new StringBuilder();
                sb.append("Product: ").append(product.getName());
                if (product.getSubTitle() != null) {
                    sb.append(", Subtitle: ").append(product.getSubTitle());
                }
                if (product.getPrice() != null) {
                    sb.append(", Price: ").append(product.getPrice());
                }
                if (product.getOriginalPrice() != null) {
                    sb.append(", Original price: ").append(product.getOriginalPrice());
                }
                if (product.getRulesJson() != null) {
                    sb.append(", Rules: ").append(product.getRulesJson());
                }
                productDetails = sb.toString();
            }
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are writing a user review for SmartLive.\n");
        prompt.append("Output language: Simplified Chinese.\n");
        prompt.append("Keep the review natural, concrete and believable. Do not invent facts that are not in the context.\n\n");
        prompt.append("Shop context: ").append(shopDetails).append("\n");
        if (productDetails != null) {
            prompt.append("Product context: ").append(productDetails).append("\n");
        }
        if (dto.getScore() != null) {
            prompt.append("Overall score: ").append(dto.getScore()).append("/5\n");
        }
        if (dto.getTasteScore() != null) {
            prompt.append("Taste score: ").append(dto.getTasteScore()).append("/5\n");
        }
        if (dto.getEnvScore() != null) {
            prompt.append("Environment score: ").append(dto.getEnvScore()).append("/5\n");
        }
        if (dto.getServiceScore() != null) {
            prompt.append("Service score: ").append(dto.getServiceScore()).append("/5\n");
        }
        if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
            prompt.append("User reference: ").append(dto.getDescription()).append("\n");
        }
        prompt.append("\nRequirements:\n");

        int score = dto.getScore() != null ? dto.getScore() : 5;
        if (score == 5) {
            prompt.append("- Tone: very positive and enthusiastic, but still natural.\n");
        } else if (score == 4) {
            prompt.append("- Tone: positive and sincere, with light detail.\n");
        } else if (score == 3) {
            prompt.append("- Tone: balanced. Mention both strengths and weaknesses.\n");
        } else {
            prompt.append("- Tone: dissatisfied but rational. State problems clearly without abuse.\n");
        }

        prompt.append("- Length: 50 to 150 Chinese characters.\n");
        prompt.append("- Write in first person from the customer perspective.\n");
        prompt.append("- Do not use markdown, JSON or bullet points.\n");

        log.info("Review Generate Prompt: {}", prompt);

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
                    throw new RuntimeException("AI review generation failed", e);
                }
            }
        }
        throw new RuntimeException("AI review generation failed after retries");
    }
}