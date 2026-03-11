package com.smartLive.ai.service.generate.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.ai.domain.DTO.BlogGenerateDTO;
import com.smartLive.ai.entity.vo.BlogGenerateVO;
import com.smartLive.ai.entity.vo.ShopVO;
import com.smartLive.ai.service.generate.IBlogGenerateService;
import com.smartLive.ai.service.rag.IReviewRagService;
import com.smartLive.ai.service.rag.IShopRagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 博客生成业务实现类
 * 使用 RAG 技术检索店铺详情及历史评价，调用大模型生成符合特定风格（探店、种草、避雷）的小红书文案
 *
 * @author smartLive
 */
@Slf4j
@Service
public class BlogGenerateServiceImpl implements IBlogGenerateService {

    private final ChatClient chatClient;
    private final IShopRagService shopRagService;
    private final IReviewRagService reviewRagService;

    @Autowired
    public BlogGenerateServiceImpl(
            @Qualifier("generalChatClient") ChatClient chatClient,
            IShopRagService shopRagService,
            IReviewRagService reviewRagService) {
        this.chatClient = chatClient;
        this.shopRagService = shopRagService;
        this.reviewRagService = reviewRagService;
    }

    /**
     * 生成博客正文
     * 1. 检索店铺详情
     * 2. 检索并汇总近期评价（作为 RAG 上下文，确保生成内容的真实性）
     * 3. 构造提示词（Prompt），包含风格要求与格式约束
     * 4. 调用大模型并对返回的 JSON 结果进行解析与重试逻辑
     *
     * @param dto 包含店铺 ID、探店要求及文案风格的 DTO
     * @return 包含 3 个标题及正文内容的 VO
     */
    @Override
    public BlogGenerateVO generate(BlogGenerateDTO dto) {
        // 1. 获取店铺基本信息
        ShopVO searchVo = new ShopVO();
        searchVo.setId(dto.getShopId());
        ShopVO shopInfo = shopRagService.getShopDetails(searchVo, "店铺详情");
        
        String shopIntro = "未知店铺";
        if (shopInfo != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("店名: ").append(shopInfo.getName()).append(", ");
            if (shopInfo.getArea() != null) sb.append("商圈: ").append(shopInfo.getArea()).append(", ");
            if (shopInfo.getAddress() != null) sb.append("地址: ").append(shopInfo.getAddress()).append(", ");
            if (shopInfo.getAvgPrice() != null) sb.append("人均: ").append(shopInfo.getAvgPrice()).append("元, ");
            if (shopInfo.getScore() > 0) sb.append("评分(满分5): ").append(shopInfo.getScore()).append("分, ");
            if (shopInfo.getOpenHours() != null) sb.append("营业时间: ").append(shopInfo.getOpenHours());
            shopIntro = sb.toString();
        }
        // 2. 从向量库获取近期评价作为参考 (避免模型幻觉/伪造)
        // sourceType = 1 (店铺评价)
        String reviewContext = reviewRagService.getReviewSummary(1, dto.getShopId(), null, null, "");

        // 3. 构建提示词
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的小红书探店博主，请根据以下信息写一篇店铺体验博客正文。\n\n");
        prompt.append("【店铺信息】\n").append(shopIntro).append("\n\n");
        
        prompt.append("【真实用户评价参考（用于提取环境、服务、口味真实亮点，禁止使用未出现的信息）】\n");
        prompt.append(reviewContext).append("\n\n");
        
        if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
            prompt.append("【本次探店明确要求】\n").append(dto.getDescription()).append("\n\n");
        }

        prompt.append("【生成要求】\n");
        int style = dto.getStyle() != null ? dto.getStyle() : 0;
        if (style == 0) {
            prompt.append("- 风格：探店笔记。生动叙述，带一点个人情感，分段落，适当使用 emoji，最好有小标题。\n");
        } else if (style == 1) {
            prompt.append("- 风格：种草推荐。突出亮点 and 优势，语气热情，强调强烈推荐。\n");
        } else if (style == 2) {
            prompt.append("- 风格：避雷测评。客观、理性指出问题或不足，语气中立。\n");
        }
        prompt.append("- 字数控制在 300-500 字，禁止编造虚假信息。\n");
        
        prompt.append("\n【重要输出格式要求】\n");
        prompt.append("请务务必输出严格的 JSON 格式，必须包含3个吸引人的候选标题以及博客正文，不要包含任何多余的解释文字或markdown标签。格式必须完全像这样:\n");
        prompt.append("{\n  \"titles\": [\"爆款标题1\", \"吸引眼球标题2\", \"优质标题3\"],\n  \"content\": \"博客正文内容...\"\n}");

        log.info("AI 博客生成提示词: {}", prompt.toString());

        // 4. 调用大模型 (带重试机制)
        ObjectMapper mapper = new ObjectMapper();
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                String result = chatClient.prompt().user(prompt.toString()).call().content();
                if (result != null && !result.isBlank()) {
                    result = result.trim();
                    // 处理可能带有的 markdown 标签
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
                log.warn("AI 生成返回内容为空或格式错误，正在重试... ({}/{})", i + 1, maxRetries);
            } catch (Exception e) {
                log.warn("AI 博客生成异常，正在重试... ({}/{})", i + 1, maxRetries, e);
                if (i == maxRetries - 1) {
                    throw new RuntimeException("AI 生成失败或返回格式解析错误，请稍后重试", e);
                }
            }
        }
        throw new RuntimeException("AI 生成未能返回有效内容，请稍后重试");
    }
}
