package com.smartLive.ai.strategy.merchant.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartLive.ai.domain.AiMerchantSession;
import com.smartLive.ai.domain.DTO.MerchantChatDTO;
import com.smartLive.ai.entity.vo.ReviewVO;
import com.smartLive.ai.service.chat.support.MerchantMessageChatMemoryManager;
import com.smartLive.ai.service.merchant.IAiMerchantMessageService;
import com.smartLive.ai.service.merchant.IAiMerchantSessionService;
import com.smartLive.ai.service.rag.IReviewRagService;
import com.smartLive.ai.service.rag.IShopRagService;
import com.smartLive.ai.strategy.merchant.AbstractMerchantAiStrategy;
import com.smartLive.shop.api.DTO.ProductSalesDTO;
import com.smartLive.shop.api.DTO.ShopAnalysisDTO;
import com.smartLive.shop.api.RemoteShopService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component("analysisStrategy")
public class AnalysisAiStrategy extends AbstractMerchantAiStrategy {

    private final IReviewRagService reviewRagService;

    public AnalysisAiStrategy(@Qualifier("merchantStrategyChatClient") ChatClient merchantStrategyChatClient,
                              IAiMerchantSessionService merchantSessionService,
                              IAiMerchantMessageService merchantMessageService,
                              MerchantMessageChatMemoryManager memoryManager,
                              RemoteShopService remoteShopService,
                              IShopRagService shopRagService,
                              ObjectMapper objectMapper,
                              IReviewRagService reviewRagService) {
        super(merchantStrategyChatClient, merchantSessionService, merchantMessageService, memoryManager,
                remoteShopService, shopRagService, objectMapper);
        this.reviewRagService = reviewRagService;
    }

    @Override
    protected String buildScenePrompt(MerchantChatDTO dto, AiMerchantSession session) {
        String normalizedDateRange = normalizeDateRange(dto.getDateRange());
        ShopAnalysisDTO analysis = convertAjaxData(
                remoteShopService.getShopAnalysis(session.getShopId(), normalizedDateRange),
                ShopAnalysisDTO.class,
                new ShopAnalysisDTO()
        );
        List<ReviewVO> badReviews = reviewRagService.getReviewsByScore(session.getShopId(), 1, 3);
        ShopPromptContext shopContext = getShopPromptContext(session.getShopId());

        return """
                场景 3：经营分析 BUSINESS_ANALYSIS

                当前任务：根据店铺经营摘要数据，输出一份简明经营分析。

                【店铺信息】
                店铺名称：%s
                店铺类型：%s
                分析周期：%s

                【经营摘要】
                营业额：%s
                订单数：%s
                客单价：%s
                支付转化率：暂无数据
                复购率：暂无数据（复购人数：%s）
                退款率：暂无数据
                差评率：暂无数据（差评数量：%s）
                平均评分：%s
                热销商品：%s
                主要差评问题：%s
                流量变化：暂无数据
                销量变化：暂无数据

                【商家补充要求】
                %s

                【输出要求】
                请按以下格式输出：
                1. 总体判断：1句话，概括当前经营状态。
                2. 核心发现：3点，每点写“现象 + 可能原因”。
                3. 经营建议：3条，按优先级排序，强调可执行性。
                4. 风险提醒：1-2条，指出接下来最需要关注的指标或问题。

                补充规则：
                1. 只能基于提供的数据分析，不得编造趋势原因。
                2. 结论要具体，避免空泛表述。
                3. 每条建议尽量落到动作层，例如“优化评价回复速度”“调整套餐展示”“提升晚高峰服务承接”。
                """.formatted(
                shopContext.getShopName(),
                shopContext.getShopType(),
                formatDateRangeLabel(normalizedDateRange),
                defaultDecimal(analysis.getTotalRevenue()),
                defaultNumber(analysis.getTotalOrders()),
                defaultDecimal(analysis.getAvgOrderPrice()),
                defaultNumber(analysis.getRepurchaseCount()),
                defaultNumber(analysis.getBadReviewCount()),
                defaultDecimal(analysis.getAvgScore()),
                formatProductSales(analysis.getHotProducts()),
                formatBadReviewSamples(badReviews),
                resolveInstruction(dto)
        );
    }

    private String formatProductSales(List<ProductSalesDTO> hotProducts) {
        if (hotProducts == null || hotProducts.isEmpty()) {
            return "暂无数据";
        }
        List<String> items = new ArrayList<>();
        for (ProductSalesDTO item : hotProducts) {
            if (item == null) {
                continue;
            }
            items.add(defaultText(item.getProductName()) + "(销量" + defaultLongNumber(item.getSalesCount()) + ")");
        }
        return items.isEmpty() ? "暂无数据" : String.join("、", items);
    }

    private String formatBadReviewSamples(List<ReviewVO> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return "暂无数据";
        }
        List<String> samples = new ArrayList<>();
        for (ReviewVO review : reviews) {
            if (review == null || !StringUtils.hasText(review.getContent())) {
                continue;
            }
            samples.add("- [" + defaultNumber(review.getScore()) + "分] " + review.getContent().trim());
            if (samples.size() >= 5) {
                break;
            }
        }
        return samples.isEmpty() ? "暂无数据" : String.join("\n", samples);
    }
}