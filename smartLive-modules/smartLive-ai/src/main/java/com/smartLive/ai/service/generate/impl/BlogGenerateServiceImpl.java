package com.smartLive.ai.service.generate.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.ai.domain.DTO.BlogGenerateDTO;
import com.smartLive.ai.entity.vo.BlogGenerateVO;
import com.smartLive.ai.entity.vo.ShopVO;
import com.smartLive.ai.service.generate.IBlogGenerateService;
import com.smartLive.ai.service.rag.IBlogRagService;
import com.smartLive.ai.service.rag.IReviewRagService;
import com.smartLive.ai.service.rag.IShopRagService;
import com.smartLive.common.core.constant.ResourceTypeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 博客生成服务实现类。
 */
@Slf4j
@Service
public class BlogGenerateServiceImpl implements IBlogGenerateService {

    private final ChatClient chatClient;
    private final IShopRagService shopRagService;
    private final IReviewRagService reviewRagService;
    private final IBlogRagService blogRagService;

    /**
     * 构造博客生成服务实现。
     */
    @Autowired
    public BlogGenerateServiceImpl(
            @Qualifier("generalChatClient") ChatClient chatClient,
            IShopRagService shopRagService,
            IReviewRagService reviewRagService,
            IBlogRagService blogRagService) {
        this.chatClient = chatClient;
        this.shopRagService = shopRagService;
        this.reviewRagService = reviewRagService;
        this.blogRagService = blogRagService;
    }

    /**
     * 生成博客生成。
     */
    @Override
    public BlogGenerateVO generate(BlogGenerateDTO dto) {
        ShopVO searchVo = new ShopVO();
        searchVo.setId(dto.getShopId());
        ShopVO shopInfo = shopRagService.getShopDetails(searchVo, "shop details");

        String shopIntro = "No shop information";
        if (shopInfo != null) {
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
            shopIntro = sb.toString();
        }

        String reviewContext = reviewRagService.getReviewSummary(
                ResourceTypeConstants.SHOP_CODE,
                dto.getShopId(),
                null,
                null,
                ""
        );
        String blogContext = blogRagService.getShopBlogSummary(dto.getShopId(), dto.getDescription(), 3);

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are writing a SmartLive blog post about a shop.\n");
        prompt.append("Output language: Simplified Chinese.\n");
        prompt.append("Use the provided facts, review summary and existing blog summary as grounding.\n");
        prompt.append("Do not fabricate prices, dishes, traffic details or review opinions beyond the context.\n\n");
        prompt.append("Shop context:\n").append(shopIntro).append("\n\n");
        prompt.append("Review summary:\n").append(reviewContext).append("\n\n");
        prompt.append("Existing blog summary:\n").append(blogContext).append("\n\n");
        if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
            prompt.append("User requirement:\n").append(dto.getDescription()).append("\n\n");
        }

        prompt.append("Writing requirements:\n");
        int style = dto.getStyle() != null ? dto.getStyle() : 0;
        if (style == 0) {
            prompt.append("- Style: natural, grounded and friendly.\n");
        } else if (style == 1) {
            prompt.append("- Style: polished, refined and suitable for sharing.\n");
        } else if (style == 2) {
            prompt.append("- Style: lively and humorous, but still readable.\n");
        }
        prompt.append("- Content length: 300 to 500 Chinese characters.\n");
        prompt.append("- Give three title options.\n");
        prompt.append("- The article should mention real highlights and overall experience.\n");
        prompt.append("- Do not output markdown code fences.\n");
        prompt.append("- Return valid JSON only with this schema:\n");
        prompt.append("{\n  \"titles\": [\"title1\", \"title2\", \"title3\"],\n  \"content\": \"blog content\"\n}");

        log.info("AI blog generate prompt: {}", prompt);

        ObjectMapper mapper = new ObjectMapper();
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                String result = chatClient.prompt().user(prompt.toString()).call().content();
                if (result != null && !result.isBlank()) {
                    result = result.trim();
                    if (result.startsWith("```json")) {
                        result = result.substring(7);
                    } else if (result.startsWith("```")) {
                        result = result.substring(3);
                    }
                    if (result.endsWith("```")) {
                        result = result.substring(0, result.length() - 3);
                    }
                    result = result.trim();

                    BlogGenerateVO vo = mapper.readValue(result, BlogGenerateVO.class);
                    if (vo != null && vo.getContent() != null && !vo.getContent().isBlank()) {
                        return vo;
                    }
                }
                log.warn("AI blog generation returned invalid content, retrying... ({}/{})", i + 1, maxRetries);
            } catch (Exception e) {
                log.warn("AI blog generation failed, retrying... ({}/{})", i + 1, maxRetries, e);
                if (i == maxRetries - 1) {
                    throw new RuntimeException("AI blog generation failed", e);
                }
            }
        }
        throw new RuntimeException("AI blog generation failed after retries");
    }
}